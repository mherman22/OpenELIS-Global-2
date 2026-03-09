import { shouldRender, parseTableData, getExtension } from "../fhirFormUtils";

describe("shouldRender", () => {
  // T027
  test("returns true when enableWhen is empty", () => {
    expect(shouldRender({ enableWhen: [] }, {})).toBe(true);
  });

  test("returns true when enableWhen is absent", () => {
    expect(shouldRender({}, {})).toBe(true);
  });

  // T028
  test("returns true when = operator matches formData value", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "=", answerCoding: { code: "YES" } }],
    };
    expect(shouldRender(item, { q1: "YES" })).toBe(true);
  });

  // T029
  test("returns false when = operator does not match", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "=", answerCoding: { code: "YES" } }],
    };
    expect(shouldRender(item, { q1: "NO" })).toBe(false);
  });

  // T030
  test("handles != operator — returns true when value differs", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "!=", answerString: "foo" }],
    };
    expect(shouldRender(item, { q1: "bar" })).toBe(true);
  });

  test("handles != operator — returns false when value equals", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "!=", answerString: "foo" }],
    };
    expect(shouldRender(item, { q1: "foo" })).toBe(false);
  });

  // T031
  test("handles exists operator — true when field has value and answerBoolean=true", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "exists", answerBoolean: true }],
    };
    expect(shouldRender(item, { q1: "someValue" })).toBe(true);
  });

  test("handles exists operator — false when field is empty and answerBoolean=true", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "exists", answerBoolean: true }],
    };
    expect(shouldRender(item, { q1: "" })).toBe(false);
  });

  test("handles exists operator — true when field is empty and answerBoolean=false", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "exists", answerBoolean: false }],
    };
    expect(shouldRender(item, {})).toBe(true);
  });

  test("returns false for unknown operator", () => {
    const item = {
      enableWhen: [{ question: "q1", operator: "~", answerString: "x" }],
    };
    expect(shouldRender(item, { q1: "x" })).toBe(false);
  });

  test("evaluates multiple enableWhen conditions with AND logic", () => {
    const item = {
      enableWhen: [
        { question: "q1", operator: "=", answerString: "A" },
        { question: "q2", operator: "=", answerString: "B" },
      ],
    };
    expect(shouldRender(item, { q1: "A", q2: "B" })).toBe(true);
    expect(shouldRender(item, { q1: "A", q2: "X" })).toBe(false);
  });
});

describe("parseTableData", () => {
  // T032
  test("returns headers from first object keys and rows with Carbon-required id field", () => {
    const data = JSON.stringify([
      { id: "r1", name: "Alice", age: 30 },
      { id: "r2", name: "Bob", age: 25 },
    ]);
    const { headers, rows } = parseTableData(data);
    expect(headers).toEqual([
      { key: "id", header: "id" },
      { key: "name", header: "name" },
      { key: "age", header: "age" },
    ]);
    expect(rows[0]).toMatchObject({ id: "r1", name: "Alice", age: 30 });
    expect(rows[1]).toMatchObject({ id: "r2", name: "Bob", age: 25 });
  });

  test("auto-assigns id as string index when id field is absent", () => {
    const data = JSON.stringify([{ name: "Alice" }, { name: "Bob" }]);
    const { rows } = parseTableData(data);
    expect(rows[0].id).toBe("0");
    expect(rows[1].id).toBe("1");
  });

  test("returns empty headers and rows for empty array", () => {
    expect(parseTableData("[]")).toEqual({ headers: [], rows: [] });
  });

  test("returns empty headers and rows for invalid JSON", () => {
    expect(parseTableData("{not json")).toEqual({ headers: [], rows: [] });
  });

  test("returns empty headers and rows for non-array JSON", () => {
    expect(parseTableData('{"a":1}')).toEqual({ headers: [], rows: [] });
  });
});

describe("getExtension", () => {
  test("returns valueString for matching URL suffix", () => {
    const item = {
      extension: [
        { url: "http://openelis-global.org/page-type", valueString: "specialized_test_assignment" },
      ],
    };
    expect(getExtension(item, "page-type")).toBe("specialized_test_assignment");
  });

  test("returns null when extension not found", () => {
    const item = { extension: [] };
    expect(getExtension(item, "page-type")).toBeNull();
  });

  test("returns null when item has no extension property", () => {
    expect(getExtension({}, "page-type")).toBeNull();
  });
});
