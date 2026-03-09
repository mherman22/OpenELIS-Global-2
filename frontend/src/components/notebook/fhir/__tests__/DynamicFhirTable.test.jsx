import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import DynamicFhirTable from "../DynamicFhirTable";

const makeItem = (rows) => ({
  linkId: "testTable",
  text: "Test Table",
  extension: [
    {
      url: "http://openelis-global.org/table-data",
      valueString: JSON.stringify(rows),
    },
  ],
});

const renderTable = (item, props = {}) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      <DynamicFhirTable item={item} selectedId={null} onSelect={jest.fn()} {...props} />
    </IntlProvider>,
  );

// T043
test("DynamicFhirTable renders column headers derived from first object keys in table-data extension", () => {
  const item = makeItem([
    { id: "1", name: "Alice", age: "30" },
    { id: "2", name: "Bob", age: "25" },
  ]);
  renderTable(item);
  expect(screen.getByText("name")).toBeInTheDocument();
  expect(screen.getByText("age")).toBeInTheDocument();
});

// T044
test("DynamicFhirTable shows Pagination when row count exceeds 10", () => {
  const rows = Array.from({ length: 12 }, (_, i) => ({
    id: String(i + 1),
    name: `Sample ${i + 1}`,
  }));
  const item = makeItem(rows);
  renderTable(item);
  // Carbon Pagination renders navigation buttons or page count
  expect(screen.getByRole("navigation")).toBeInTheDocument();
});

// T045
test("DynamicFhirTable filters rows by search term across all columns (client-side, no fetch)", () => {
  const item = makeItem([
    { id: "1", name: "Alice", city: "Paris" },
    { id: "2", name: "Bob", city: "London" },
    { id: "3", name: "Charlie", city: "Paris" },
  ]);
  renderTable(item);

  const searchInput = screen.getByRole("searchbox");
  fireEvent.change(searchInput, { target: { value: "Bob" } });

  expect(screen.getByText("Bob")).toBeInTheDocument();
  expect(screen.queryByText("Alice")).not.toBeInTheDocument();
  expect(screen.queryByText("Charlie")).not.toBeInTheDocument();
});

// T046
test("DynamicFhirTable calls onSelect with row id when row is clicked", () => {
  const onSelect = jest.fn();
  const item = makeItem([{ id: "row-1", name: "Alice" }]);
  renderTable(item, { onSelect });

  // Find the expand row button or the row itself and click
  const rows = screen.getAllByRole("row");
  // rows[0] is header, rows[1] is the data row
  fireEvent.click(rows[1]);

  expect(onSelect).toHaveBeenCalledWith("row-1");
});

// T047
test("DynamicFhirTable renders empty state message when table-data array is empty", () => {
  const item = makeItem([]);
  renderTable(item);
  expect(screen.getByText(/No data available\./i)).toBeInTheDocument();
});
