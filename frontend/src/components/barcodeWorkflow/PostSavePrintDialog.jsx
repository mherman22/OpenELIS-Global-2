import React from "react";
import { Button, InlineLoading, Stack, Tile } from "@carbon/react";
import { Checkmark, Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

const normalize = (label) => {
  if (typeof label === "string") {
    return { labelType: label, quantity: 1, dimensionsMm: "", printUrl: "" };
  }
  return {
    labelType: label?.labelType ?? "",
    quantity: label?.quantity ?? 0,
    dimensionsMm: label?.dimensionsMm ?? "",
    printUrl: label?.printUrl ?? "",
  };
};

const formatType = (type) =>
  type ? `${type.charAt(0).toUpperCase()}${type.slice(1)} label` : "";

const styles = {
  tile: {
    padding: "1.25rem 1.5rem",
  },
  header: {
    display: "flex",
    flexDirection: "column",
    gap: "0.25rem",
  },
  caption: {
    margin: 0,
    fontSize: "0.75rem",
    fontWeight: 500,
    color: "#6f6f6f",
    textTransform: "uppercase",
    letterSpacing: "0.06em",
  },
  accession: {
    fontFamily: "'IBM Plex Mono', monospace",
    fontSize: "1.05rem",
    fontWeight: 700,
    color: "#161616",
    wordBreak: "break-all",
  },
  row: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: "1rem",
  },
  meta: {
    minWidth: 0,
  },
  type: {
    fontSize: "0.95rem",
    fontWeight: 600,
    color: "#161616",
    lineHeight: 1.25,
  },
  qty: {
    fontSize: "0.8rem",
    color: "#6f6f6f",
    marginTop: "0.15rem",
  },
  footer: {
    display: "flex",
    justifyContent: "flex-end",
  },
};

const PostSavePrintDialog = ({
  accessionNumber,
  printableLabelTypes = [],
  onPrint,
  onDone,
  isLoading = false,
}) => {
  const intl = useIntl();

  if (!accessionNumber) return null;

  const labels = printableLabelTypes.map(normalize);

  const handlePrint = (label) => {
    if (onPrint) {
      onPrint(label.labelType, label.quantity);
      return;
    }
    if (label.printUrl) {
      window.open(label.printUrl);
    }
  };

  // Default Done = navigate home so the button is never a no-op when the
  // caller forgets to pass onDone.
  const handleDone = () => {
    if (onDone) {
      onDone();
      return;
    }
    window.location.href = "/";
  };

  return (
    <Tile style={styles.tile}>
      <Stack gap={5}>
        <header style={styles.header}>
          <span style={styles.caption}>
            <FormattedMessage id="barcode.print.dialog.title" />
          </span>
          <span style={styles.accession}>{accessionNumber}</span>
        </header>

        {labels.length > 0 && (
          <Stack gap={4}>
            {labels.map((label) => (
              <div key={label.labelType} style={styles.row}>
                <div style={styles.meta}>
                  <div style={styles.type}>{formatType(label.labelType)}</div>
                  <div style={styles.qty}>
                    {intl.formatMessage({
                      id: "label.quantity",
                      defaultMessage: "Qty",
                    })}
                    {": "}
                    {label.quantity}
                    {label.dimensionsMm ? ` · ${label.dimensionsMm}` : ""}
                  </div>
                </div>
                <Button
                  size="sm"
                  renderIcon={Printer}
                  onClick={() => handlePrint(label)}
                >
                  <FormattedMessage id="barcode.print.button" />
                </Button>
              </div>
            ))}
          </Stack>
        )}

        {isLoading && <InlineLoading />}

        <div style={styles.footer}>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleDone}
          >
            <FormattedMessage
              id={
                labels.length > 0
                  ? "barcode.print.done"
                  : "barcode.print.skip"
              }
            />
          </Button>
        </div>
      </Stack>
    </Tile>
  );
};

export default PostSavePrintDialog;
