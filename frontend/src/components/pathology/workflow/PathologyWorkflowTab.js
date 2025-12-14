import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Loading,
  Grid,
  Column,
  Button,
  Tag,
  ProgressBar,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "../../notebook/workflow/PageNavigation";
import PathologySampleReceptionPage from "./pages/PathologySampleReceptionPage";
import PathologyQCPage from "./pages/PathologyQCPage";
import PathologyTestingPage from "./pages/PathologyTestingPage";
import PathologyProcessingPage from "./pages/PathologyProcessingPage";
import "./PathologyWorkflow.css";

/**
 * Default pathology workflow pages.
 * Maps to the 10 workflow templates created in Liquibase (IDs 100-109)
 */
const DEFAULT_PATHOLOGY_PAGES = [
  {
    id: "pathology-1",
    order: 1,
    title: "Sample Reception & Registration",
    pageType: "INDIVIDUAL_PROCESSING",
  },
  {
    id: "pathology-2",
    order: 2,
    title: "Initial Sample Inspection",
    pageType: "BATCH_VERIFICATION",
  },
  {
    id: "pathology-3",
    order: 3,
    title: "Grossing & Block Preparation",
    pageType: "INDIVIDUAL_PROCESSING",
  },
  {
    id: "pathology-4",
    order: 4,
    title: "Tissue Block QC",
    pageType: "BATCH_VERIFICATION",
  },
  {
    id: "pathology-5",
    order: 5,
    title: "Microtomy & Slide Preparation",
    pageType: "BULK_DATA_ENTRY",
  },
  {
    id: "pathology-6",
    order: 6,
    title: "Staining & Processing",
    pageType: "BULK_DATA_ENTRY",
  },
  {
    id: "pathology-7",
    order: 7,
    title: "Slide QC & Control Validation",
    pageType: "BATCH_VERIFICATION",
  },
  {
    id: "pathology-8",
    order: 8,
    title: "Microscopic Examination",
    pageType: "INDIVIDUAL_PROCESSING",
  },
  {
    id: "pathology-9",
    order: 9,
    title: "Pathologist Review & Sign-off",
    pageType: "INDIVIDUAL_PROCESSING",
  },
  {
    id: "pathology-10",
    order: 10,
    title: "Results Release & Archive",
    pageType: "BATCH_VERIFICATION",
  },
];

/**
 * PathologyWorkflowTab - Container component for pathology workflow pages.
 * Displays the 10-page pathology workflow with progress indicators and navigation.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The pathology notebook template ID
 * @param {number} props.entryId - The notebook entry ID
 */
function PathologyWorkflowTab({ notebookId, entryId: propEntryId }) {
  const componentMounted = useRef(false);
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [notebook, setNotebook] = useState(null);
  const [entry, setEntry] = useState(null);
  const [entryId, setEntryId] = useState(propEntryId);
  const [pages, setPages] = useState([]);
  const [pageProgress, setPageProgress] = useState({});
  const [activePage, setActivePage] = useState(0);
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);

  // Use actual pages if available, otherwise use default workflow pages
  const effectivePages = useMemo(() => {
    if (pages && pages.length > 0) {
      return pages;
    }
    return DEFAULT_PATHOLOGY_PAGES;
  }, [pages]);

  useEffect(() => {
    componentMounted.current = true;
    loadNotebookData();

    return () => {
      componentMounted.current = false;
    };
  }, [notebookId, propEntryId]);

  const loadNotebookData = () => {
    if (!notebookId && !propEntryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    // Load notebook/entry data
    const endpoint = propEntryId
      ? `/rest/notebook/entry/${propEntryId}`
      : `/rest/notebook/${notebookId}`;

    getFromOpenElisServer(endpoint, (response) => {
      if (componentMounted.current) {
        if (response) {
          if (propEntryId) {
            setEntry(response);
            setNotebook(response.notebook);
            setPages(response.notebook?.pages || []);
          } else {
            setNotebook(response);
            setPages(response.pages || []);
          }
          loadPageProgress();
        } else {
          setErrorMessage("Failed to load pathology workflow");
        }
        setLoading(false);
      }
    });
  };

  const loadPageProgress = useCallback(() => {
    if (!effectivePages || effectivePages.length === 0) return;

    // Load progress for each page
    effectivePages.forEach((page) => {
      // Skip synthetic page IDs
      if (String(page.id).startsWith("pathology-")) {
        return;
      }

      getFromOpenElisServer(
        `/rest/notebook/page/${page.id}/progress`,
        (progressData) => {
          if (componentMounted.current) {
            setPageProgress((prev) => ({
              ...prev,
              [page.id]: progressData,
            }));
          }
        },
      );
    });
  }, [effectivePages]);

  const handleProgressUpdate = useCallback((pageId) => {
    // Reload progress for the specific page
    if (String(pageId).startsWith("pathology-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/page/${pageId}/progress`,
      (progressData) => {
        if (componentMounted.current) {
          setPageProgress((prev) => ({
            ...prev,
            [pageId]: progressData,
          }));
        }
      },
    );
  }, []);

  const renderPageComponent = (page, index) => {
    const progress = pageProgress[page.id] || {
      total: 0,
      pending: 0,
      inProgress: 0,
      completed: 0,
      skipped: 0,
      percentage: 0,
    };

    // Map pages to appropriate components based on page type or order
    switch (index) {
      case 0: // Sample Reception
        return (
          <PathologySampleReceptionPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 1: // Initial QC
      case 3: // Block QC
      case 6: // Slide QC
        return (
          <PathologyQCPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            qcType={
              index === 1
                ? "INITIAL_INSPECTION"
                : index === 3
                  ? "BLOCK_QC"
                  : "SLIDE_QC"
            }
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 2: // Grossing & Block Preparation
        return (
          <PathologyProcessingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            processingType="GROSSING"
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 4: // Microtomy & Slide Preparation
        return (
          <PathologyProcessingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            processingType="MICROTOMY"
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 5: // Staining & Processing
        return (
          <PathologyProcessingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            processingType="STAINING"
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 7: // Testing
        return (
          <PathologyTestingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 8: // Pathologist Review & Sign-off
        return (
          <PathologyProcessingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            processingType="PATHOLOGIST_REVIEW"
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      case 9: // Results Release & Archive
        return (
          <PathologyProcessingPage
            entryId={entryId}
            pageData={page}
            progress={progress}
            processingType="RESULTS_RELEASE"
            onProgressUpdate={() => handleProgressUpdate(page.id)}
          />
        );

      default:
        // Fallback placeholder
        return (
          <Grid>
            <Column lg={16}>
              <h3>{page.title}</h3>
              <p>
                <FormattedMessage
                  id="pathology.workflow.placeholder"
                  defaultMessage="This page is under development."
                />
              </p>
              <ProgressBar
                label={`${progress.completed} / ${progress.total} samples completed`}
                value={progress.percentage}
                max={100}
              />
            </Column>
          </Grid>
        );
    }
  };

  if (loading) {
    return (
      <Loading
        description="Loading pathology workflow..."
        withOverlay={false}
      />
    );
  }

  if (errorMessage) {
    return (
      <Grid>
        <Column lg={16}>
          <p className="error-message">{errorMessage}</p>
        </Column>
      </Grid>
    );
  }

  return (
    <div className="pathology-workflow-container">
      <Grid fullWidth>
        <Column lg={16}>
          <h2>
            <FormattedMessage
              id="pathology.workflow.title"
              defaultMessage="Pathology Laboratory Workflow"
            />
          </h2>
          {notebook && (
            <p className="notebook-description">{notebook.description}</p>
          )}
        </Column>
      </Grid>

      <PageNavigation
        pages={effectivePages}
        activePage={activePage}
        onPageChange={setActivePage}
        pageProgress={pageProgress}
      />

      <Tabs
        selectedIndex={activePage}
        onChange={({ selectedIndex }) => setActivePage(selectedIndex)}
      >
        <TabList aria-label="Pathology workflow pages" activation="manual">
          {effectivePages.map((page, index) => {
            const progress = pageProgress[page.id] || {};
            const completionTag =
              progress.total > 0 ? (
                <Tag
                  type={progress.percentage === 100 ? "green" : "blue"}
                  size="sm"
                >
                  {progress.completed}/{progress.total}
                </Tag>
              ) : null;

            return (
              <Tab key={page.id}>
                {page.title} {completionTag}
              </Tab>
            );
          })}
        </TabList>

        <TabPanels>
          {effectivePages.map((page, index) => (
            <TabPanel key={page.id}>
              {activePage === index && renderPageComponent(page, index)}
            </TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </div>
  );
}

export default PathologyWorkflowTab;
