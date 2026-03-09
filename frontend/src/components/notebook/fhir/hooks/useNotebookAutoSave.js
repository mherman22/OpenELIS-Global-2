import { useEffect, useRef, useState } from "react";

/**
 * useNotebookAutoSave — debounced auto-save hook for FHIR QuestionnaireResponse.
 *
 * Behaviour:
 *  - Does NOT fire on initial mount.
 *  - After any formData change, waits 2 seconds before POSTing.
 *  - Rapid changes reset the 2-second timer (debounce).
 *  - Timer is cleared on unmount to prevent memory leaks.
 *  - Does not fire when entryId, pageId, or specimenId are missing.
 *
 * @param {Object}           formData      - current form state keyed by linkId
 * @param {Object}           questionnaire - FHIR Questionnaire (must be truthy to enable saves)
 * @param {string|number}    entryId       - NotebookEntry ID
 * @param {string|number}    pageId        - NotebookPage ID
 * @param {string}           specimenId    - specimen/sample identifier
 * @returns {{ draftStatus: 'idle'|'saving'|'saved'|'error' }}
 */
export function useNotebookAutoSave(
  formData,
  questionnaire,
  entryId,
  pageId,
  specimenId,
) {
  const [draftStatus, setDraftStatus] = useState("idle");
  const isInitialMount = useRef(true);
  const timerRef = useRef(null);

  useEffect(() => {
    // Skip the initial mount — only save on subsequent formData changes
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }

    // All required params must be present
    if (!questionnaire || !entryId || !pageId || !specimenId) return;

    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(async () => {
      setDraftStatus("saving");
      try {
        await fetch(
          `/rest/notebook/entry/${entryId}/page/${pageId}/questionnaire-response`,
          {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              status: "in-progress",
              specimenId,
              responses: formData,
            }),
          },
        );
        setDraftStatus("saved");
      } catch {
        setDraftStatus("error");
      }
    }, 2000);

    return () => clearTimeout(timerRef.current);
  }, [formData]); // eslint-disable-line react-hooks/exhaustive-deps

  return { draftStatus };
}
