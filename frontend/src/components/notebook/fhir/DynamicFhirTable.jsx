import React, { useState, useMemo } from "react";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableExpandedRow,
  TableExpandHeader,
  TableExpandRow,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Pagination,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { parseTableData, getExtension } from "./fhirFormUtils";

const PAGE_SIZE = 10;

/**
 * DynamicFhirTable — Carbon DataTable rendered from a FHIR item's
 * `table-data` extension (JSON array string).
 *
 * Features:
 *  - Client-side search across all columns via TableToolbarSearch
 *  - Expand row to reveal detail columns (columns 5+)
 *  - Pagination auto-shown when row count exceeds PAGE_SIZE
 *  - Row selection calls onSelect(row.id)
 *
 * @param {Object}   props.item       - FHIR Questionnaire item with table-data extension
 * @param {string}   props.selectedId - currently selected row id
 * @param {function} props.onSelect   - callback(rowId) when a row is clicked
 */
export default function DynamicFhirTable({ item, selectedId, onSelect }) {
  const [searchTerm, setSearchTerm] = useState("");
  const [page, setPage] = useState(1);

  const tableDataStr = getExtension(item, "table-data");
  const { headers, rows } = useMemo(
    () => parseTableData(tableDataStr ?? "[]"),
    [tableDataStr],
  );

  const filteredRows = useMemo(() => {
    if (!searchTerm) return rows;
    const term = searchTerm.toLowerCase();
    return rows.filter((row) =>
      headers.some((h) => String(row[h.key] ?? "").toLowerCase().includes(term)),
    );
  }, [rows, headers, searchTerm]);

  const pagedRows = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredRows.slice(start, start + PAGE_SIZE);
  }, [filteredRows, page]);

  if (headers.length === 0) {
    return (
      <p className="fhir-table-empty">
        <FormattedMessage
          id="notebook.fhir.table.emptyState"
          defaultMessage="No data available."
        />
      </p>
    );
  }

  // Main columns: first 4 headers; detail: the rest
  const mainHeaders = headers.slice(0, 4);
  const detailHeaders = headers.slice(4);

  return (
    <div>
      <DataTable rows={pagedRows} headers={mainHeaders}>
        {({
          rows: dtRows,
          headers: dtHeaders,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <TableContainer title={item.text}>
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  onChange={(e) => {
                    setSearchTerm(e.target.value);
                    setPage(1);
                  }}
                />
              </TableToolbarContent>
            </TableToolbar>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  <TableExpandHeader />
                  {dtHeaders.map((header) => (
                    <TableHeader key={header.key} {...getHeaderProps({ header })}>
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {dtRows.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableExpandRow
                      {...getRowProps({ row })}
                      isSelected={row.id === selectedId}
                      onClick={() => onSelect?.(row.id)}
                    >
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableExpandRow>
                    <TableExpandedRow colSpan={dtHeaders.length + 1}>
                      <dl>
                        {detailHeaders.map((h) => (
                          <React.Fragment key={h.key}>
                            <dt>{h.header}</dt>
                            <dd>
                              {pagedRows.find((r) => r.id === row.id)?.[h.key] ?? ""}
                            </dd>
                          </React.Fragment>
                        ))}
                      </dl>
                    </TableExpandedRow>
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      {filteredRows.length > PAGE_SIZE && (
        <Pagination
          totalItems={filteredRows.length}
          pageSize={PAGE_SIZE}
          pageSizes={[PAGE_SIZE, 20, 50]}
          page={page}
          onChange={({ page: p }) => setPage(p)}
        />
      )}
    </div>
  );
}
