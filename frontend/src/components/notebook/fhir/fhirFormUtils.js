/**
 * Evaluate FHIR enableWhen conditions against current form data.
 * All conditions must be satisfied (AND logic per FHIR R4 default behavior).
 *
 * @param {Object} item     - FHIR Questionnaire item
 * @param {Object} formData - current form state keyed by linkId
 * @returns {boolean} true if the item should be rendered
 */
export function shouldRender(item, formData) {
  if (!item.enableWhen || item.enableWhen.length === 0) return true;
  return item.enableWhen.every((condition) => {
    const userAnswer = formData[condition.question];
    const target =
      condition.answerCoding?.code ??
      condition.answerString ??
      (condition.answerBoolean !== undefined
        ? condition.answerBoolean
        : undefined);
    switch (condition.operator) {
      case "=":
        return userAnswer === target;
      case "!=":
        return userAnswer !== target;
      case "exists":
        return condition.answerBoolean ? !!userAnswer : !userAnswer;
      default:
        return false;
    }
  });
}

/**
 * Parse the table-data FHIR extension string into Carbon DataTable format.
 *
 * @param {string} extensionValueString - JSON array string
 * @returns {{ headers: Array<{key, header}>, rows: Array }}
 */
export function parseTableData(extensionValueString) {
  try {
    const data = JSON.parse(extensionValueString);
    if (!Array.isArray(data) || data.length === 0) return { headers: [], rows: [] };
    const headers = Object.keys(data[0]).map((key) => ({ key, header: key }));
    const rows = data.map((row, idx) => ({
      id: row.id ?? String(idx),
      ...row,
    }));
    return { headers, rows };
  } catch {
    return { headers: [], rows: [] };
  }
}

/**
 * Get extension value by URL suffix from a FHIR item's extensions array.
 *
 * @param {Object} item      - FHIR item with optional extension array
 * @param {string} urlSuffix - suffix to match against extension.url
 * @returns {string|null} valueString of the first matching extension, or null
 */
export function getExtension(item, urlSuffix) {
  return (
    item.extension?.find((e) => e.url?.includes(urlSuffix))?.valueString ?? null
  );
}
