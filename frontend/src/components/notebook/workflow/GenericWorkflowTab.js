import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
} from "react";
import {
  Loading,
  Grid,
  Column,
  Tag,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { usePageAccessControl } from "../../../hooks/usePageAccessControl";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import PageNavigation from "./PageNavigation";
import PAGE_TYPE_REGISTRY from "./PAGE_TYPE_REGISTRY";
import "./NotebookWorkflow.css";

/**
 * GenericWorkflowTab - Single workflow tab for ALL notebook labs.
 *
 * Dispatches each page to the correct React component by looking up
 * page.pageType in PAGE_TYPE_REGISTRY. No domain-specific tab file is needed
 * per lab — only page component files + a CSV entry in
 * volume/configuration/backend/notebook-page-types/<lab>.csv.
 *
 * @param {Object} props
 * @param {number} props.notebookId - The notebook instance ID
 * @param {number} [props.entryId]  - Direct entry ID (optional)
 */
function GenericWorkflowTab({ notebookId, entryId: propEntryId }) {
  const componentMounted = useRef(false);
  const intl = useIntl();
  const { setNotificationVisible } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [notebook, setNotebook] = useState(null);
  const [entry, setEntry] = useState(null);
  const [entryId, setEntryId] = useState(propEntryId);
  const [pages, setPages] = useState([]);
  const [pageProgress, setPageProgress] = useState({});
  const [samples, setSamples] = useState([]);
  const [errorMessage, setErrorMessage] = useState(null);
  const [syncing, setSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState(null);
  const [isCreatingEntry, setIsCreatingEntry] = useState(!propEntryId);

  const { effectivePages, activePage, handlePageChange } = usePageAccessControl(
    pages,
    [],
    0,
    { isCreating: isCreatingEntry },
  );

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
    if (propEntryId) {
      loadEntryData(propEntryId);
    } else if (notebookId) {
      loadNotebookAndEntry(notebookId);
    }
  };

  const loadEntryData = (eId) => {
    let loadCount = 0;
    const checkDone = () => {
      loadCount++;
      if (loadCount >= 2) setLoading(false);
    };

    getFromOpenElisServer(`/rest/notebook-entry/${eId}`, (response) => {
      if (componentMounted.current && response) {
        setEntry(response);
        setEntryId(eId);
        if (response.notebook) {
          getFromOpenElisServer(
            `/rest/notebook/view/${response.notebook.id}`,
            (nbResponse) => {
              if (componentMounted.current && nbResponse) {
                setNotebook(nbResponse);
                setPages(nbResponse.pages || []);
              }
            },
          );
        }
      }
      checkDone();
    });

    getFromOpenElisServer(`/rest/notebook-entry/${eId}/samples`, (response) => {
      if (componentMounted.current && response) {
        setSamples(response || []);
      }
      checkDone();
    });
  };

  const loadNotebookAndEntry = (nbId) => {
    getFromOpenElisServer(`/rest/notebook/view/${nbId}`, (nbResponse) => {
      if (componentMounted.current && nbResponse) {
        setNotebook(nbResponse);
        setPages(nbResponse.pages || []);

        getFromOpenElisServer(
          `/rest/notebook-entry/by-notebook/${nbId}`,
          (entriesResponse) => {
            if (componentMounted.current) {
              if (
                entriesResponse &&
                Array.isArray(entriesResponse) &&
                entriesResponse.length > 0
              ) {
                const existingEntry = entriesResponse[0];
                setEntry(existingEntry);
                setEntryId(existingEntry.id);
                setIsCreatingEntry(false);

                getFromOpenElisServer(
                  `/rest/notebook-entry/${existingEntry.id}/samples`,
                  (samplesResponse) => {
                    if (componentMounted.current) {
                      setSamples(
                        Array.isArray(samplesResponse) ? samplesResponse : [],
                      );
                    }
                    setLoading(false);
                  },
                );
              } else {
                setIsCreatingEntry(true);
                createEntryForNotebook(nbId);
              }
            }
          },
        );
      } else {
        setLoading(false);
      }
    });
  };

  const createEntryForNotebook = (nbId) => {
    fetch(
      `${config.serverBaseUrl}/rest/notebook-entry/create?notebookId=${nbId}`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then(async (response) => {
        const text = await response.text();
        let data = {};
        try {
          data = text ? JSON.parse(text) : {};
        } catch (e) {
          console.error("Failed to parse response as JSON:", e);
        }
        if (!response.ok) {
          throw new Error(
            data.error || `HTTP ${response.status}: ${response.statusText}`,
          );
        }
        return data;
      })
      .then((data) => {
        if (componentMounted.current) {
          if (data && data.id) {
            setEntry(data);
            setEntryId(data.id);
            setSamples([]);
            setIsCreatingEntry(false);
          }
          setLoading(false);
        }
      })
      .catch((error) => {
        console.error("Failed to create notebook entry:", error.message);
        if (componentMounted.current) {
          setErrorMessage(error.message);
          setLoading(false);
        }
      });
  };

  const handleProgressUpdate = useCallback(() => {
    if (entryId) {
      getFromOpenElisServer(
        `/rest/notebook-entry/${entryId}/samples`,
        (response) => {
          if (componentMounted.current && response) {
            setSamples(response || []);
          }
        },
      );
    }
  }, [entryId]);

  const handleSyncPages = useCallback(() => {
    if (!entryId) return;
    setSyncing(true);
    setSyncMessage(null);

    fetch(`${config.serverBaseUrl}/rest/notebook-entry/${entryId}/sync-pages`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
    })
      .then((response) => response.json())
      .then((data) => {
        if (componentMounted.current) {
          if (data.success) {
            if (data.pagesAdded > 0) {
              setSyncMessage({
                kind: "success",
                text: intl.formatMessage(
                  {
                    id: "notebook.workflow.syncSuccess",
                    defaultMessage:
                      "{count} new page(s) added from template. Refreshing...",
                  },
                  { count: data.pagesAdded },
                ),
              });
              setTimeout(() => {
                loadNotebookData();
                setSyncMessage(null);
              }, 1500);
            } else {
              setSyncMessage({
                kind: "info",
                text: intl.formatMessage({
                  id: "notebook.workflow.syncNoChanges",
                  defaultMessage:
                    "All pages are already in sync with the template.",
                }),
              });
              setTimeout(() => setSyncMessage(null), 3000);
            }
          } else {
            setSyncMessage({
              kind: "error",
              text: data.error || "Sync failed",
            });
          }
        }
      })
      .catch((error) => {
        if (componentMounted.current) {
          setSyncMessage({
            kind: "error",
            text: error.message || "Failed to sync pages",
          });
        }
      })
      .finally(() => {
        if (componentMounted.current) setSyncing(false);
      });
  }, [entryId, intl]);

  const getProgressForPage = (pageId) => {
    const progress = pageProgress[pageId];
    return progress || { total: 0, completed: 0, percentage: 0 };
  };

  const renderPageContent = (page) => {
    const PageComponent = PAGE_TYPE_REGISTRY[page.pageType];
    const progress = getProgressForPage(page.id);
    const pageProps = {
      entryId,
      pageData: page,
      progress,
      onProgressUpdate: handleProgressUpdate,
      notebookId: notebook?.id,
      notebookData: notebook,
      templateInstruments: notebook?.analyzers,
    };

    if (PageComponent) {
      return <PageComponent key={`page-${page.id}`} {...pageProps} />;
    }

    return (
      <div className="page-placeholder">
        <FormattedMessage
          id="notebook.workflow.pageDefault.description"
          defaultMessage="Page content for workflow step {step}"
          values={{ step: page.order || page.pageOrder }}
        />
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading workflow..." />
      </div>
    );
  }

  if (!entry && !notebook) {
    return (
      <div
        className="workflow-error"
        style={{ padding: "2rem", color: "#525252" }}
      >
        <FormattedMessage
          id="notebook.workflow.notFound"
          defaultMessage="Notebook not found. Please select a valid notebook."
        />
      </div>
    );
  }

  if (notebook && !entryId) {
    return (
      <div
        className="workflow-error"
        style={{ padding: "2rem", color: "#525252" }}
      >
        <FormattedMessage
          id="notebook.workflow.entryCreationFailed"
          defaultMessage="Failed to create notebook entry. Please refresh and try again."
        />
        {errorMessage && (
          <div
            style={{
              marginTop: "1rem",
              fontSize: "0.875rem",
              color: "#da1e28",
            }}
          >
            Error: {errorMessage}
          </div>
        )}
      </div>
    );
  }

  const displayTitle = entry?.title || notebook?.title;
  const displayStatus = entry?.status || notebook?.status;

  return (
    <div className="notebook-workflow-container">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="workflow-header">
            <h2>{displayTitle}</h2>
            <div className="workflow-meta">
              <Tag type="blue">{displayStatus}</Tag>
              <span className="sample-count">
                <FormattedMessage
                  id="notebook.workflow.sampleCount"
                  values={{ count: samples.length }}
                />
              </span>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={handleSyncPages}
                disabled={syncing || !entryId}
                style={{ marginLeft: "1rem" }}
              >
                {syncing ? (
                  <FormattedMessage
                    id="notebook.workflow.syncing"
                    defaultMessage="Syncing..."
                  />
                ) : (
                  <FormattedMessage
                    id="notebook.workflow.syncPages"
                    defaultMessage="Sync Pages"
                  />
                )}
              </Button>
            </div>
          </div>
          {syncMessage && (
            <InlineNotification
              kind={syncMessage.kind}
              title=""
              subtitle={syncMessage.text}
              lowContrast
              onCloseButtonClick={() => setSyncMessage(null)}
              style={{ marginTop: "0.5rem" }}
            />
          )}
        </Column>
      </Grid>

      <Grid fullWidth className="workflow-content">
        <Column lg={4} md={2} sm={4}>
          <PageNavigation
            pages={effectivePages}
            activePage={activePage}
            onPageChange={handlePageChange}
            pageProgress={pageProgress}
          />
        </Column>

        <Column lg={12} md={6} sm={4}>
          <div className="workflow-page-content">
            {effectivePages.length > 0 &&
              effectivePages[activePage] &&
              effectivePages[activePage].hasAccess && (
                <div className="page-panel">
                  <div className="page-header">
                    <h3>{effectivePages[activePage].title}</h3>
                    <div className="page-progress">
                      {(() => {
                        const progress = getProgressForPage(
                          effectivePages[activePage].id,
                        );
                        return (
                          <span>
                            {progress.completed}/{progress.total}{" "}
                            <FormattedMessage id="notebook.workflow.samplesCompleted" />
                          </span>
                        );
                      })()}
                    </div>
                  </div>

                  <div className="page-content">
                    {effectivePages[activePage].instructions && (
                      <div className="page-instructions">
                        {effectivePages[activePage].instructions}
                      </div>
                    )}

                    <div key={`page-content-${effectivePages[activePage].id}`}>
                      {renderPageContent(effectivePages[activePage])}
                    </div>
                  </div>
                </div>
              )}

            {effectivePages.length > 0 &&
              effectivePages[activePage] &&
              !effectivePages[activePage].hasAccess && (
                <div className="page-panel access-denied">
                  <div className="page-header">
                    <h3>{effectivePages[activePage].title}</h3>
                    <Tag type="red">
                      <FormattedMessage
                        id="notebook.page.restricted"
                        defaultMessage="Restricted"
                      />
                    </Tag>
                  </div>
                  <div className="page-content">
                    <p>
                      <FormattedMessage
                        id="notebook.page.accessDeniedMessage"
                        defaultMessage="You do not have the required role to access this page."
                      />
                    </p>
                  </div>
                </div>
              )}
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default GenericWorkflowTab;
