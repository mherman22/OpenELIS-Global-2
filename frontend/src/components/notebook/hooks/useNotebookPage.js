import { useState, useEffect, useRef, useCallback } from "react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";

/**
 * useNotebookPage - Shared hook for all notebook workflow page components.
 *
 * Eliminates the ~80 lines of identical boilerplate repeated across every
 * lab-specific page (sample loading, state management, routing, saving).
 *
 * @param {string|number} entryId         - Notebook entry ID
 * @param {Object}        pageData        - Notebook page object (includes id, data)
 * @param {function}      onProgressUpdate - Called when page progress changes
 * @param {function}      [transformSample] - Optional (sample) => transformed sample.
 *   Called per-sample after loading. Spread sample.data by default; override to add
 *   lab-specific derived fields.
 *
 * @returns {{
 *   samples: Array,
 *   setSamples: function,
 *   selectedSampleIds: string[],
 *   setSelectedSampleIds: function,
 *   statusFilter: string,
 *   setStatusFilter: function,
 *   loading: boolean,
 *   error: string|null,
 *   setError: function,
 *   successMessage: string|null,
 *   setSuccessMessage: function,
 *   hasRealPageId: boolean,
 *   componentMounted: React.RefObject<boolean>,
 *   loadPageSamples: function,
 *   routeSamplesToPage: function(sampleIds: string[], targetPageOrder: number, pages: Object[]),
 *   applyBulkData: function(sampleIds: string[], data: Object, onSuccess?: function, onError?: function),
 *   updateBulkStatus: function(sampleIds: string[], status: string, onSuccess?: function, onError?: function),
 * }}
 */
export function useNotebookPage(
  entryId,
  pageData,
  onProgressUpdate,
  transformSample,
) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const hasRealPageId =
    !!pageData?.id && !String(pageData.id).startsWith("default-");

  // Default sample transform: spread sample.data so extra fields are top-level
  const defaultTransform = useCallback(
    (sample) => ({
      ...(sample.data || {}),
      id: String(sample.id || sample.sampleItemId),
      externalId: sample.externalId,
      accessionNumber: sample.accessionNumber,
      sampleType: sample.sampleType || sample.typeOfSample?.description,
      collectionDate: sample.collectionDate,
      status: sample.pageStatus || sample.status || "PENDING",
      patientName: sample.patientName,
      volume: sample.volume,
    }),
    [],
  );

  const transform = transformSample || defaultTransform;

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setSamples(response.map(transform));
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id, transform]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  /**
   * Route sample IDs to a target page by page order number.
   * Used after QC decisions to move samples to the next stage.
   */
  const routeSamplesToPage = useCallback(
    (sampleIds, targetPageOrder, pages) => {
      if (!pages || sampleIds.length === 0) return;

      const targetPage = pages.find((p) => p.order === targetPageOrder);
      if (targetPage && !String(targetPage.id).startsWith("default-")) {
        postToOpenElisServer(
          `/rest/notebook/bulk/page/${targetPage.id}/samples/add`,
          JSON.stringify({
            sampleIds: sampleIds.map((id) => parseInt(id, 10)),
          }),
          () => {},
        );
      }
    },
    [],
  );

  /**
   * Converts a sample ID to a number if it represents a plain integer,
   * or leaves it as a string for composite IDs (e.g. "101-2").
   */
  const coerceSampleId = useCallback((id) => {
    const n = Number(id);
    return Number.isInteger(n) && String(n) === String(id) ? n : id;
  }, []);

  /**
   * POST arbitrary data fields to selected samples.
   * Used by QC pages, processing pages, etc.
   * Endpoint: POST /rest/notebook/bulk/page/:pageId/samples/apply
   */
  const applyBulkData = useCallback(
    (sampleIds, data, onSuccess, onError) => {
      if (!hasRealPageId) {
        const msg = intl.formatMessage({
          id: "notebook.error.pageNotReady",
          defaultMessage:
            "Cannot update samples: page not properly initialized. Please refresh.",
        });
        setError(msg);
        if (onError) onError(msg);
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify({
          sampleIds: sampleIds.map(coerceSampleId),
          data,
        }),
        (status) => {
          if (componentMounted.current) {
            if (status === 200) {
              loadPageSamples();
              setSelectedSampleIds([]);
              if (onProgressUpdate) onProgressUpdate();
              if (onSuccess) onSuccess();
            } else {
              const msg = intl.formatMessage({
                id: "notebook.error.saveFailed",
                defaultMessage: "Failed to save. Please try again.",
              });
              setError(msg);
              if (onError) onError(msg);
            }
          }
        },
      );
    },
    [
      hasRealPageId,
      pageData?.id,
      loadPageSamples,
      onProgressUpdate,
      coerceSampleId,
      intl,
    ],
  );

  /**
   * POST a status change for selected samples.
   * Endpoint: POST /rest/notebook/bulk/page/:pageId/samples/status
   */
  const updateBulkStatus = useCallback(
    (sampleIds, status, onSuccess, onError) => {
      if (!hasRealPageId) {
        const msg = intl.formatMessage({
          id: "notebook.error.pageNotReady",
          defaultMessage:
            "Cannot update samples: page not properly initialized. Please refresh.",
        });
        setError(msg);
        if (onError) onError(msg);
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: sampleIds.map(coerceSampleId),
          status,
        }),
        (httpStatus) => {
          if (componentMounted.current) {
            if (httpStatus === 200) {
              loadPageSamples();
              setSelectedSampleIds([]);
              if (onProgressUpdate) onProgressUpdate();
              if (onSuccess) onSuccess();
            } else {
              const msg = intl.formatMessage({
                id: "notebook.error.statusFailed",
                defaultMessage: "Failed to update status. Please try again.",
              });
              setError(msg);
              if (onError) onError(msg);
            }
          }
        },
      );
    },
    [
      hasRealPageId,
      pageData?.id,
      loadPageSamples,
      onProgressUpdate,
      coerceSampleId,
      intl,
    ],
  );

  return {
    samples,
    setSamples,
    selectedSampleIds,
    setSelectedSampleIds,
    statusFilter,
    setStatusFilter,
    loading,
    error,
    setError,
    successMessage,
    setSuccessMessage,
    hasRealPageId,
    componentMounted,
    loadPageSamples,
    routeSamplesToPage,
    applyBulkData,
    updateBulkStatus,
  };
}

export default useNotebookPage;
