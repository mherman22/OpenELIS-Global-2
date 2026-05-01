import React from "react";
import {
  Button,
  InlineLoading,
  Stack,
  StructuredList,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  Tag,
  Tile,
} from "@carbon/react";
import { Checkmark, Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import "./PostSavePrintDialog.scss";

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
    <Tile>
      <Stack gap={5}>
        <div>
          <p className="post-save-dialog__caption">
            <FormattedMessage id="barcode.print.dialog.title" />
          </p>
          <Tag
            type="cool-gray"
            className="post-save-dialog__accession-tag"
          >
            {accessionNumber}
          </Tag>
        </div>

        {labels.length > 0 && (
          <StructuredList condensed flush>
            <StructuredListBody>
              {labels.map((label) => (
                <StructuredListRow key={label.labelType}>
                  <StructuredListCell>
                    <p className="post-save-dialog__label-name">
                      {formatType(label.labelType)}
                    </p>
                    <p className="post-save-dialog__label-qty">
                      {intl.formatMessage({
                        id: "label.quantity",
                        defaultMessage: "Quantity",
                      })}
                      {": "}
                      {label.quantity}
                      {label.dimensionsMm ? ` · ${label.dimensionsMm}` : ""}
                    </p>
                  </StructuredListCell>
                  <StructuredListCell className="post-save-dialog__action-cell">
                    <Button
                      size="sm"
                      renderIcon={Printer}
                      onClick={() => handlePrint(label)}
                    >
                      <FormattedMessage id="barcode.print.button" />
                    </Button>
                  </StructuredListCell>
                </StructuredListRow>
              ))}
            </StructuredListBody>
          </StructuredList>
        )}

        {isLoading && <InlineLoading />}

        <Button kind="primary" renderIcon={Checkmark} onClick={handleDone}>
          <FormattedMessage
            id={
              labels.length > 0 ? "barcode.print.done" : "barcode.print.skip"
            }
          />
        </Button>
      </Stack>
    </Tile>
  );
};

export default PostSavePrintDialog;
