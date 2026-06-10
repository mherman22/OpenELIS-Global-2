import React, { Fragment, useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  DatePicker,
  DatePickerInput,
  InlineLoading,
  InlineNotification,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { ChevronDown, ChevronRight, DocumentDownload, Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServerForBlob } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "./LaporanHasil.css";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "nav.reports.laporanHasil", link: "/LaporanHasil" },
];

function ComplianceStatusTag({ status, passCount, marginalCount, failCount }) {
  if (status === "COMPLIANT") {
    return (
      <>
        <Tag type="green" size="sm">
          &#10003; <FormattedMessage id="label.laporanHasil.allCompliant" defaultMessage="All Compliant" />
        </Tag>
        {passCount != null && (
          <div className="lh-pf-counts">{passCount}P / {marginalCount}M / {failCount}F</div>
        )}
      </>
    );
  }
  if (status === "BORDERLINE") {
    return (
      <>
        <Tag type="yellow" size="sm">
          &#9888; <FormattedMessage id="label.laporanHasil.marginal" defaultMessage="Marginal" />
        </Tag>
        {passCount != null && (
          <div className="lh-pf-counts">{passCount}P / {marginalCount}M / {failCount}F</div>
        )}
      </>
    );
  }
  return (
    <>
      <Tag type="red" size="sm">
        &#10007; <FormattedMessage id="label.laporanHasil.nonCompliant" defaultMessage="Non-Compliant" />
      </Tag>
      {passCount != null && (
        <div className="lh-pf-counts">{passCount}P / {marginalCount}M / {failCount}F</div>
      )}
    </>
  );
}

function GenerationStatusTag({ certificateNumber, lastGeneratedAt }) {
  if (lastGeneratedAt) {
    return (
      <>
        <Tag type="green" size="sm">
          <FormattedMessage id="label.laporanHasil.generated" defaultMessage="Generated" />
        </Tag>
        <div className="lh-gen-detail">{lastGeneratedAt}</div>
        {certificateNumber && <div className="lh-gen-detail">{certificateNumber}</div>}
      </>
    );
  }
  return (
    <Tag type="gray" size="sm">
      <FormattedMessage id="label.laporanHasil.notGenerated" defaultMessage="Not Yet Generated" />
    </Tag>
  );
}

function MiniComplianceTable({ parameters }) {
  if (!parameters || parameters.length === 0) return null;
  return (
    <table className="lh-mini-table">
      <thead>
        <tr>
          <th><FormattedMessage id="label.laporanHasil.preview.parameter" defaultMessage="Parameter" /></th>
          <th><FormattedMessage id="label.laporanHasil.preview.result" defaultMessage="Result" /></th>
          <th><FormattedMessage id="label.laporanHasil.preview.threshold" defaultMessage="Threshold" /></th>
          <th><FormattedMessage id="label.laporanHasil.preview.status" defaultMessage="Status" /></th>
        </tr>
      </thead>
      <tbody>
        {parameters.map((p, i) => (
          <tr key={i}>
            <td>{p.name}</td>
            <td>{p.value}</td>
            <td>{p.threshold}</td>
            <td>
              {p.status === "PASS" && (
                <Tag type="green" size="sm">&#10003; <FormattedMessage id="label.pdf.compliant" defaultMessage="Compliant" /></Tag>
              )}
              {p.status === "MARGINAL" && (
                <Tag type="yellow" size="sm">&#9888; <FormattedMessage id="label.pdf.marginal" defaultMessage="Marginal" /></Tag>
              )}
              {p.status === "FAIL" && (
                <Tag type="red" size="sm">&#10007; <FormattedMessage id="label.pdf.nonCompliant" defaultMessage="Non-Compliant" /></Tag>
              )}
              {p.status === "NOT_TESTED" && (
                <Tag type="cool-gray" size="sm"><FormattedMessage id="label.laporanHasil.notTested" defaultMessage="Not Tested" /></Tag>
              )}
              {p.status === "UNKNOWN" && (
                <Tag type="gray" size="sm"><FormattedMessage id="label.laporanHasil.unknownStatus" defaultMessage="No Result" /></Tag>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ExpandPanel({ orderId, preview }) {
  const intl = useIntl();

  if (!preview) {
    return (
      <div className="lh-expand-panel">
        <InlineLoading description={intl.formatMessage({ id: "loading.description", defaultMessage: "Loading..." })} />
      </div>
    );
  }

  const { labNumber, siteName, collectionDate, standardName, collectionConditions, parameters, signatures } = preview;

  const siteInfo = [
    [intl.formatMessage({ id: "label.laporanHasil.labNumber", defaultMessage: "Lab Number" }), labNumber],
    [intl.formatMessage({ id: "label.laporanHasil.site", defaultMessage: "Site" }), siteName],
    [intl.formatMessage({ id: "label.laporanHasil.collectionDate", defaultMessage: "Collection Date" }), collectionDate],
    [intl.formatMessage({ id: "label.laporanHasil.standard", defaultMessage: "Standard" }), standardName],
  ];

  function sigRole(role) {
    if (role === "AUTHORED") return intl.formatMessage({ id: "label.laporanHasil.preview.testedBy", defaultMessage: "Tested by" });
    if (role === "VALIDATED_AND_RELEASED") return intl.formatMessage({ id: "label.laporanHasil.preview.approvedBy", defaultMessage: "Approved by" });
    return role || "";
  }

  return (
    <div className="lh-expand-panel">
      <div className="lh-expand-grid">
        {/* Left column */}
        <div>
          <h4 className="lh-section-head">
            &#128205; <FormattedMessage id="heading.laporanHasil.preview.siteInfo" defaultMessage="Site Information" />
          </h4>
          <div className="lh-preview-card">
            {siteInfo.map(([label, value]) => (
              <div key={label} className="lh-preview-row">
                <span className="lh-pr-label">{label}</span>
                <span className="lh-pr-value">{value}</span>
              </div>
            ))}
          </div>
          {collectionConditions && collectionConditions.length > 0 && (
            <>
              <h4 className="lh-section-head lh-section-head--mt">
                &#129516; <FormattedMessage id="heading.laporanHasil.preview.conditions" defaultMessage="Collection Conditions" />
              </h4>
              <div className="lh-preview-card">
                {collectionConditions.map((c) => (
                  <div key={c.label} className="lh-preview-row">
                    <span className="lh-pr-label">{c.label}</span>
                    <span className="lh-pr-value">{c.value}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
        {/* Right column */}
        <div>
          <h4 className="lh-section-head">
            &#128737; <FormattedMessage id="heading.laporanHasil.preview.compliance" defaultMessage="Compliance Summary" />
          </h4>
          <div className="lh-preview-card">
            <MiniComplianceTable parameters={parameters} />
          </div>
          {signatures && signatures.length > 0 && (
            <>
              <h4 className="lh-section-head lh-section-head--mt">
                &#9997; <FormattedMessage id="heading.laporanHasil.preview.signatures" defaultMessage="E-Signatures" />
              </h4>
              <div className="lh-preview-card">
                <div className="lh-sig-grid">
                  {signatures.map((sig) => (
                    <div key={sig.role || sig.name}>
                      <div className="lh-sig-label">{sigRole(sig.role)}</div>
                      <div className="lh-sig-name">{sig.name}</div>
                      <div className="lh-sig-time">{sig.timestamp}</div>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default function LaporanHasil() {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(true);

  /* ---- Filters ---- */
  const today = new Date();
  const thirtyDaysAgo = new Date(today);
  thirtyDaysAgo.setDate(today.getDate() - 30);
  const fmt = (d) => d.toISOString().split("T")[0];

  const [dateFrom, setDateFrom] = useState(fmt(thirtyDaysAgo));
  const [dateTo, setDateTo] = useState(fmt(today));
  const [siteFilter, setSiteFilter] = useState("");
  const [standardFilter, setStandardFilter] = useState("");
  const [complianceFilter, setComplianceFilter] = useState("");
  const [generationFilter, setGenerationFilter] = useState("");

  /* ---- Data ---- */
  const [loading, setLoading] = useState(false);
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState({ total: 0, generated: 0, notGenerated: 0 });
  const [sites, setSites] = useState([]);
  const [standards, setStandards] = useState([]);
  const [complianceStatuses, setComplianceStatuses] = useState([]);

  /* ---- Table state ---- */
  const [selectedRows, setSelectedRows] = useState(new Set());
  const [expandedRows, setExpandedRows] = useState(new Set());
  const [previews, setPreviews] = useState({});

  /* ---- Generating state ---- */
  const [generatingRows, setGeneratingRows] = useState(new Set());
  const [batchGenerating, setBatchGenerating] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    loadFilterOptions();
    return () => { componentMounted.current = false; };
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [dateFrom, dateTo, siteFilter, standardFilter, complianceFilter, generationFilter]);

  function loadFilterOptions() {
    getFromOpenElisServer("/rest/admin/vector/sampling-sites/active", (data) => {
      if (!componentMounted.current) return;
      setSites(Array.isArray(data) ? data : []);
    });
    getFromOpenElisServer("/rest/compliance/standards/active", (data) => {
      if (!componentMounted.current) return;
      setStandards(Array.isArray(data) ? data : []);
    });
    getFromOpenElisServer("/rest/compliance/reports/compliance-statuses", (data) => {
      if (!componentMounted.current) return;
      setComplianceStatuses(Array.isArray(data) ? data : []);
    });
  }

  function fetchOrders() {
    setLoading(true);
    const params = new URLSearchParams();
    if (dateFrom) params.set("fromDate", dateFrom);
    if (dateTo) params.set("toDate", dateTo);
    if (siteFilter) params.set("siteId", siteFilter);
    if (standardFilter) params.set("standardId", standardFilter);
    if (complianceFilter) params.set("complianceStatus", complianceFilter);
    if (generationFilter) params.set("status", generationFilter);

    getFromOpenElisServer(`/rest/compliance/reports/eligible-orders?${params}`, (data) => {
      if (!componentMounted.current) return;
      setLoading(false);
      if (!data) return;
      setOrders(data.orders || []);
      setSummary({
        total: data.totalEligible || 0,
        generated: data.totalGenerated || 0,
        notGenerated: data.totalNotGenerated || 0,
      });
    });
  }

  function loadPreview(orderId) {
    if (previews[orderId] !== undefined) return;
    setPreviews((prev) => ({ ...prev, [orderId]: null }));
    getFromOpenElisServer(`/rest/compliance/reports/eligible-orders/${orderId}/preview`, (data) => {
      if (!componentMounted.current) return;
      setPreviews((prev) => ({ ...prev, [orderId]: data || {} }));
    });
  }

  function toggleExpand(orderId) {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(orderId)) {
        next.delete(orderId);
      } else {
        next.add(orderId);
        loadPreview(orderId);
      }
      return next;
    });
  }

  function toggleSelect(orderId) {
    setSelectedRows((prev) => {
      const next = new Set(prev);
      if (next.has(orderId)) next.delete(orderId);
      else next.add(orderId);
      return next;
    });
  }

  function toggleSelectAll(checked) {
    if (checked) setSelectedRows(new Set(orders.map((o) => o.id)));
    else setSelectedRows(new Set());
  }

  function clearSelection() {
    setSelectedRows(new Set());
  }

  function triggerDownload(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  function handleGenerate(order) {
    setGeneratingRows((prev) => new Set(prev).add(order.id));
    postToOpenElisServerForBlob(
      "/rest/compliance/reports/generate",
      JSON.stringify({ orderId: order.id }),
      (blob) => {
        if (!componentMounted.current) return;
        setGeneratingRows((prev) => { const s = new Set(prev); s.delete(order.id); return s; });
        const certNum = order.lastCertificateNumber || "cert";
        const filename = `LHU-${certNum}_${order.siteName}_${order.labNumber}.pdf`;
        triggerDownload(blob, filename);
        fetchOrders();
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "message.laporanHasil.success", defaultMessage: "Certificate generated successfully." }),
        });
      },
      () => {
        if (!componentMounted.current) return;
        setGeneratingRows((prev) => { const s = new Set(prev); s.delete(order.id); return s; });
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "message.laporanHasil.error", defaultMessage: "Failed to generate certificate." }),
        });
      },
    );
  }

  function handleBatchDownload() {
    if (selectedRows.size === 0) return;
    if (selectedRows.size > 50) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "message.laporanHasil.batchLimit", defaultMessage: "Batch generation is limited to 50 orders. Please select fewer orders." }),
      });
      return;
    }
    setBatchGenerating(true);
    const today = new Date();
    const dateStr = fmt(today);
    const filename = `Laporan_Hasil_${dateStr}_${selectedRows.size}certificates.zip`;
    postToOpenElisServerForBlob(
      "/rest/compliance/reports/generate-batch",
      JSON.stringify({ orderIds: Array.from(selectedRows) }),
      (blob) => {
        if (!componentMounted.current) return;
        setBatchGenerating(false);
        triggerDownload(blob, filename);
        fetchOrders();
        clearSelection();
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage(
            { id: "message.laporanHasil.batchSuccess", defaultMessage: "{count} certificates generated." },
            { count: selectedRows.size },
          ),
        });
      },
      () => {
        if (!componentMounted.current) return;
        setBatchGenerating(false);
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "message.laporanHasil.error", defaultMessage: "Batch generation failed." }),
        });
      },
    );
  }

  const allSelected = orders.length > 0 && orders.every((o) => selectedRows.has(o.id));
  const someSelected = selectedRows.size > 0;

  return (
    <div className="orderLegendBody">
      {/* Page header */}
      <div className="lh-page-header">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <h1 className="lh-page-title">
          <FormattedMessage id="heading.laporanHasil.title" defaultMessage="Laporan Hasil — Compliance Report" />
        </h1>
        <p className="lh-page-subtitle">
          <FormattedMessage id="heading.laporanHasil.subtitle" defaultMessage="Generate Sertifikat Hasil Uji (Test Results Certificates) for validated environmental orders" />
        </p>
      </div>

      <div className="lh-content">
        {/* Summary bar */}
        <div className="lh-summary-bar">
          <div className="lh-summary-card">
            <div className="lh-icon-circle" style={{ background: "rgba(15,98,254,0.08)", color: "var(--cds-interactive)" }}>
              &#128230;
            </div>
            <div>
              <div className="lh-card-value">{summary.total}</div>
              <div className="lh-card-label">
                <FormattedMessage id="label.laporanHasil.totalEligible" defaultMessage="Total Eligible" />
              </div>
            </div>
          </div>
          <div className="lh-summary-card">
            <div className="lh-icon-circle" style={{ background: "rgba(36,161,72,0.08)", color: "var(--cds-support-success)" }}>
              &#10003;
            </div>
            <div>
              <div className="lh-card-value">{summary.generated}</div>
              <div className="lh-card-label">
                <FormattedMessage id="label.laporanHasil.generated" defaultMessage="Generated" />
              </div>
            </div>
          </div>
          <div className="lh-summary-card">
            <div className="lh-icon-circle" style={{ background: "rgba(111,111,111,0.08)", color: "var(--cds-text-secondary)" }}>
              &#9203;
            </div>
            <div>
              <div className="lh-card-value">{summary.notGenerated}</div>
              <div className="lh-card-label">
                <FormattedMessage id="label.laporanHasil.notGeneratedCount" defaultMessage="Not Yet Generated" />
              </div>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="lh-filters">
          <div className="lh-filter-field">
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={dateFrom}
              onChange={([d]) => d && setDateFrom(fmt(d))}
            >
              <DatePickerInput
                id="lh-date-from"
                labelText={intl.formatMessage({ id: "label.laporanHasil.filter.dateFrom", defaultMessage: "Date From" })}
                placeholder="yyyy-mm-dd"
                size="sm"
              />
            </DatePicker>
          </div>
          <div className="lh-filter-field">
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={dateTo}
              onChange={([d]) => d && setDateTo(fmt(d))}
            >
              <DatePickerInput
                id="lh-date-to"
                labelText={intl.formatMessage({ id: "label.laporanHasil.filter.dateTo", defaultMessage: "Date To" })}
                placeholder="yyyy-mm-dd"
                size="sm"
              />
            </DatePicker>
          </div>
          <div className="lh-filter-field">
            <Select
              id="lh-filter-site"
              labelText={intl.formatMessage({ id: "label.laporanHasil.filter.site", defaultMessage: "Sampling Site" })}
              size="sm"
              value={siteFilter}
              onChange={(e) => setSiteFilter(e.target.value)}
            >
              <SelectItem value="" text={intl.formatMessage({ id: "label.laporanHasil.filter.allSites", defaultMessage: "All Sites" })} />
              {sites.map((s) => (
                <SelectItem key={s.id} value={String(s.id)} text={s.name} />
              ))}
            </Select>
          </div>
          <div className="lh-filter-field">
            <Select
              id="lh-filter-standard"
              labelText={intl.formatMessage({ id: "label.laporanHasil.filter.standard", defaultMessage: "Compliance Standard" })}
              size="sm"
              value={standardFilter}
              onChange={(e) => setStandardFilter(e.target.value)}
            >
              <SelectItem value="" text={intl.formatMessage({ id: "label.laporanHasil.filter.allStandards", defaultMessage: "All Standards" })} />
              {standards.map((s) => (
                <SelectItem key={s.id} value={String(s.id)} text={s.name} />
              ))}
            </Select>
          </div>
          <div className="lh-filter-field">
            <Select
              id="lh-filter-compliance"
              labelText={intl.formatMessage({ id: "label.laporanHasil.filter.complianceStatus", defaultMessage: "Compliance Status" })}
              size="sm"
              value={complianceFilter}
              onChange={(e) => setComplianceFilter(e.target.value)}
            >
              <SelectItem value="" text={intl.formatMessage({ id: "label.laporanHasil.filter.statusAll", defaultMessage: "All" })} />
              {complianceStatuses.map((s) => (
                <SelectItem key={s.name} value={s.name} text={s.displayName} />
              ))}
            </Select>
          </div>
          <div className="lh-filter-field">
            <Select
              id="lh-filter-generation"
              labelText={intl.formatMessage({ id: "label.laporanHasil.filter.generationStatus", defaultMessage: "Generation Status" })}
              size="sm"
              value={generationFilter}
              onChange={(e) => setGenerationFilter(e.target.value)}
            >
              <SelectItem value="" text={intl.formatMessage({ id: "label.laporanHasil.filter.genAll", defaultMessage: "All" })} />
              <SelectItem value="notGenerated" text={intl.formatMessage({ id: "label.laporanHasil.filter.genNotYet", defaultMessage: "Not Yet Generated" })} />
              <SelectItem value="generated" text={intl.formatMessage({ id: "label.laporanHasil.filter.genPrevious", defaultMessage: "Previously Generated" })} />
            </Select>
          </div>
        </div>

        {/* Batch bar */}
        <div className={`lh-batch-bar${someSelected ? "" : " lh-batch-bar--hidden"}`}>
          <span className="lh-batch-count">
            <FormattedMessage
              id="label.laporanHasil.batchSelected"
              defaultMessage="{count} items selected"
              values={{ count: selectedRows.size }}
            />
          </span>
          <div className="lh-batch-spacer" />
          <button
            className="lh-batch-btn"
            disabled={batchGenerating}
            onClick={() => {
              Array.from(selectedRows).forEach((id) => {
                const order = orders.find((o) => o.id === id);
                if (order) handleGenerate(order);
              });
            }}
          >
            <DocumentDownload size={16} />
            <FormattedMessage id="button.laporanHasil.generateBatch" defaultMessage="Generate PDFs" />
          </button>
          <button
            className="lh-batch-btn"
            disabled={batchGenerating}
            onClick={handleBatchDownload}
          >
            {batchGenerating ? (
              <InlineLoading description="" style={{ width: "auto" }} />
            ) : (
              <Download size={16} />
            )}
            <FormattedMessage id="button.laporanHasil.downloadZip" defaultMessage="Download ZIP" />
          </button>
          <button className="lh-batch-btn lh-batch-btn--outline" onClick={clearSelection}>
            &#10005; <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
          </button>
        </div>

        {/* Table */}
        <div className="lh-table-wrap">
          {loading ? (
            <InlineLoading description={intl.formatMessage({ id: "loading.description", defaultMessage: "Loading..." })} />
          ) : (
            <Table size="md" useZebraStyles={false}>
              <TableHead>
                <TableRow>
                  <TableHeader style={{ width: "2rem" }}>
                    <Checkbox
                      id="lh-select-all"
                      labelText=""
                      hideLabel
                      checked={allSelected}
                      indeterminate={someSelected && !allSelected}
                      onChange={(_, { checked }) => toggleSelectAll(checked)}
                    />
                  </TableHeader>
                  <TableHeader style={{ width: "2rem" }} />
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.labNumber" defaultMessage="Lab Number" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.site" defaultMessage="Site" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.standard" defaultMessage="Standard" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.collectionDate" defaultMessage="Collection Date" />
                  </TableHeader>
                  <TableHeader style={{ textAlign: "center" }}>
                    <FormattedMessage id="label.laporanHasil.tests" defaultMessage="Tests" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.compliance" defaultMessage="Compliance" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.laporanHasil.lastGenerated" defaultMessage="Last Generated" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.actions" defaultMessage="Actions" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={10} style={{ textAlign: "center", color: "var(--cds-text-secondary)", padding: "2rem" }}>
                      <FormattedMessage id="message.laporanHasil.noOrders" defaultMessage="No eligible orders found for the selected filters." />
                    </TableCell>
                  </TableRow>
                )}
                {orders.map((order) => {
                  const isExpanded = expandedRows.has(order.id);
                  const isSelected = selectedRows.has(order.id);
                  const isGenerating = generatingRows.has(order.id);
                  return (
                    <Fragment key={order.id}>
                      <TableRow
                        className={`lh-row-clickable${isSelected ? " lh-row-selected" : ""}`}
                        onClick={() => toggleExpand(order.id)}
                      >
                        <TableCell onClick={(e) => e.stopPropagation()}>
                          <Checkbox
                            id={`lh-sel-${order.id}`}
                            labelText=""
                            hideLabel
                            checked={isSelected}
                            onChange={() => toggleSelect(order.id)}
                          />
                        </TableCell>
                        <TableCell>
                          <span className="lh-chevron">
                            {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                          </span>
                        </TableCell>
                        <TableCell className="lh-lab-number">{order.labNumber}</TableCell>
                        <TableCell>
                          {order.siteName}
                          <br />
                          <span className="lh-site-code">{order.siteName}</span>
                        </TableCell>
                        <TableCell style={{ fontSize: "0.8125rem" }}>{order.standardName}</TableCell>
                        <TableCell>{order.collectionDate}</TableCell>
                        <TableCell className="lh-test-count">
                          {order.testCountRequired > 0
                            ? `${order.testCount} / ${order.testCountRequired}`
                            : order.testCount}
                        </TableCell>
                        <TableCell>
                          <ComplianceStatusTag
                            status={order.complianceStatus}
                            passCount={order.passCount}
                            marginalCount={order.marginalCount}
                            failCount={order.failCount}
                          />
                        </TableCell>
                        <TableCell>
                          <GenerationStatusTag
                            certificateNumber={order.lastCertificateNumber}
                            lastGeneratedAt={order.lastGeneratedAt}
                          />
                        </TableCell>
                        <TableCell onClick={(e) => e.stopPropagation()}>
                          <Button
                            kind="primary"
                            size="sm"
                            className="lh-btn-generate"
                            renderIcon={DocumentDownload}
                            iconDescription={intl.formatMessage({ id: "button.laporanHasil.generate", defaultMessage: "Generate PDF" })}
                            disabled={isGenerating}
                            onClick={() => handleGenerate(order)}
                          >
                            {isGenerating ? (
                              <FormattedMessage id="message.laporanHasil.generating" defaultMessage="Generating..." />
                            ) : (
                              <FormattedMessage id="button.laporanHasil.generate" defaultMessage="Generate PDF" />
                            )}
                          </Button>
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow>
                          <TableCell colSpan={10} style={{ padding: 0 }}>
                            <ExpandPanel orderId={order.id} preview={previews[order.id]} />
                          </TableCell>
                        </TableRow>
                      )}
                    </Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </div>
      </div>
    </div>
  );
}
