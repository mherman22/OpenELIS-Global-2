import useSWR from "swr";

/**
 * Fetch a FHIR Questionnaire resource for a given notebook.
 *
 * The endpoint (`GET /rest/notebook/${notebookId}/questionnaire`) is
 * implemented in M1 (NotebookFhirController). Until M1 is merged, the hook
 * returns `{ questionnaire: undefined, isLoading: false, error: <Error> }`.
 *
 * Deduplication interval is 5 minutes — questionnaires rarely change at
 * runtime; the template is only updated when the admin deploys a new JSON.
 */
const fetcher = (url) =>
  fetch(url, { credentials: "include" }).then((r) => {
    if (!r.ok) throw new Error("Failed to load questionnaire");
    return r.json();
  });

/**
 * @param {string|number|null} notebookId - ID of the notebook whose questionnaire to fetch
 * @returns {{ questionnaire: Object|undefined, isLoading: boolean, error: Error|undefined, mutate: function }}
 */
export function useNotebookFhirTemplate(notebookId) {
  const { data, error, isLoading, mutate } = useSWR(
    notebookId ? `/rest/notebook/${notebookId}/questionnaire` : null,
    fetcher,
    { revalidateOnFocus: false, dedupingInterval: 300000 },
  );
  return { questionnaire: data, isLoading, error, mutate };
}
