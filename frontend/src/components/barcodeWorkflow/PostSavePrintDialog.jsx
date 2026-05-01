import React from "react";
import { Button, InlineLoading, Stack, Tile } from "@carbon/react";
import { Printer, Checkmark } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

const normalizePrintableLabel = (printableLabel) => {
  if (typeof printableLabel === "string") {
    return {
      labelType: printableLabel,
      quantity: 1,
      dimensionsMm: "",
      printUrl: "",
    };
  }

  return {
    labelType: printableLabel?.labelType || "",
    quantity: printableLabel?.quantity ?? 0,
    dimensionsMm: printableLabel?.dimensionsMm || "",
    printUrl: printableLabel?.printUrl || "",
  };
};

const formatLabelType = (labelType) => {
  if (!labelType) return "";
  return labelType.charAt(0).toUpperCase() + labelType.slice(1) + " label";
};

const PostSavePrintDialog = ({
  accessionNumber,
  printableLabelTypes = [],
  onPrint = undefined,
  onDone = undefined,
  isLoading = false,
}) => {
  const intl = useIntl();

  if (!accessionNumber) {
    return null;
  }

  const printableLabels = printableLabelTypes.map(normalizePrintableLabel);

  const handlePrint = (printableLabel) => {
    if (onPrint) {
      onPrint(printableLabel.labelType, printableLabel.quantity);
      return;
    }

    if (printableLabel.printUrl) {
      window.open(printableLabel.printUrl);
    }
  };

  // Default Done = navigate home so the button is never a no-op when the
  // caller forgets to pass onDone (the historical behavior across every
  // consumer of this dialog).
  const handleDone = () => {
    if (onDone) {
      onDone();
      return;
    }
    window.location.href = "/";
  };

  const styles = {
    title: {
      margin: 0,
      fontSize: "0.95rem",
      fontWeight: 600,
      color: "#161616",
      lineHeight: 1.3,
    },
    accession: {
      fontFamily: "'IBM Plex Mono', monospace",
      fontWeight: 700,
      whiteSpace: "nowrap",
    },
    row: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      gap: "1rem",
    },
    rowMeta: {
      display: "flex",
      flexDirection: "column",
      lineHeight: 1.25,
      minWidth: 0,
    },
    rowType: {
      fontWeight: 600,
      color: "#161616",
      whiteSpace: "nowrap",
    },
    rowQty: {
      fontSize: "0.85rem",
      color: "#525252",
    },
    printButton: {
      flexShrink: 0,
    },
    footer: {
      display: "flex",
      justifyContent: "flex-end",
      marginTop: "0.5rem",
    },
  };

  return (
    <Tile>
      <Stack gap={4}>
        <h4 style={styles.title}>
          <FormattedMessage id="barcode.print.dialog.title" />
          {": "}
          <span style={styles.accession}>{accessionNumber}</span>
        </h4>

        {printableLabels.length > 0 && (
          <Stack gap={3}>
            {printableLabels.map((printableLabel) => (
              <div key={printableLabel.labelType} style={styles.row}>
                <div style={styles.rowMeta}>
                  <span style={styles.rowType}>
                    {formatLabelType(printableLabel.labelType)}
                  </span>
                  <span style={styles.rowQty}>
                    {intl.formatMessage({
                      id: "label.quantity",
                      defaultMessage: "Qty",
                    })}
                    {": "}
                    {printableLabel.quantity}
                    {printableLabel.dimensionsMm
                      ? ` · ${printableLabel.dimensionsMm}`
                      : ""}
                  </span>
                </div>
                <div style={styles.printButton}>
                  <Button
                    size="sm"
                    renderIcon={Printer}
                    onClick={() => handlePrint(printableLabel)}
                  >
                    <FormattedMessage id="barcode.print.button" />
                  </Button>
                </div>
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
                printableLabels.length > 0
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
