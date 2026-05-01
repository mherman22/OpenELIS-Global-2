import React, { useEffect } from "react";
import { Button, Row, Stack } from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";

const OrderSuccessMessage = (props) => {
  const {
    orderFormValues,
    setOrderFormValues,
    setSamples,
    setPage,
    saveResponse,
  } = props;

  const dialogModel = saveResponse?.postSavePrintDialog;
  const accessionNumber =
    dialogModel?.accessionNumber || orderFormValues.sampleOrderItems.labNo;
  const printableTypes =
    dialogModel?.printableLabelTypes &&
    dialogModel.printableLabelTypes.length > 0
      ? dialogModel.printableLabelTypes
      : ["order"];
  // Pass through whatever quantities the backend computed from the user's
  // LabelsSection input on the AddSample form. The backend's printUrl already
  // embeds quantity + override=true once BarcodeWorkflowPrintService is fixed,
  // so prefer it; fall back to a locally constructed URL only if absent.
  const printableLabels = printableTypes.map((labelType) => {
    const isObject = typeof labelType !== "string";
    const normalizedType = isObject ? labelType.labelType : labelType;
    const quantity = isObject && labelType.quantity > 0 ? labelType.quantity : 1;
    const backendUrl = isObject ? labelType.printUrl : "";
    const fallbackUrl =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${accessionNumber}&type=${normalizedType}` +
      `&quantity=${quantity}&override=true`;
    return {
      labelType: normalizedType,
      quantity,
      printUrl: backendUrl || fallbackUrl,
    };
  });

  const handlePrintByType = (labelType, quantity = 1) => {
    const safeQuantity = quantity > 0 ? quantity : 1;
    const printUrl =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${accessionNumber}&type=${labelType}` +
      `&quantity=${safeQuantity}&override=true`;
    window.open(printUrl);
  };

  const handleAnotherSiteOrder = () => {
    const siteId = orderFormValues.sampleOrderItems.referringSiteId;
    const siteName = orderFormValues.sampleOrderItems.referringSiteName;
    const providerId = orderFormValues.sampleOrderItems.providerId;
    const providerFirstName =
      orderFormValues.sampleOrderItems.providerFirstName;
    const providerLastName = orderFormValues.sampleOrderItems.providerLastName;
    const providerWorkPhone =
      orderFormValues.sampleOrderItems.providerWorkPhone;
    const providerFax = orderFormValues.sampleOrderItems.providerFax;
    const providerEmail = orderFormValues.sampleOrderItems.providerEmail;

    setOrderFormValues(SampleOrderFormValues);

    setOrderFormValues({
      ...SampleOrderFormValues,
      rememberSiteAndRequester: true,
      sampleOrderItems: {
        ...SampleOrderFormValues.sampleOrderItems,
        referringSiteId: siteId,
        referringSiteName: siteName,
        providerId: providerId,
        providerFirstName: providerFirstName,
        providerLastName: providerLastName,
        providerWorkPhone: providerWorkPhone,
        providerFax: providerFax,
        providerEmail: providerEmail,
      },
    });
    setPage(0);
  };

  useEffect(() => {
    if (!orderFormValues.rememberSiteAndRequester) {
      setOrderFormValues(SampleOrderFormValues);
    }
    setSamples([sampleObject]);
  }, []);

  return (
    <div className="orderLegendBody">
      <div className="orderEntrySuccessMsg">
        <Stack gap={4} className="orderEntrySuccessHeader">
          <CheckmarkFilled
            size={64}
            className="orderEntrySuccessIcon"
            aria-label="Order Entry saved successfully"
          />
          <h4 className="orderEntrySuccessTitle">
            <FormattedMessage id="save.success" />
          </h4>
        </Stack>
        <div className="orderEntrySuccessPrintPanel">
          <PostSavePrintDialog
            accessionNumber={accessionNumber}
            printableLabelTypes={printableLabels}
            onPrint={handlePrintByType}
          />
        </div>
        <Row className="orderEntrySuccessActions">
          {orderFormValues.rememberSiteAndRequester && (
            <Button
              className="placeAnotherOrderBtn"
              kind="tertiary"
              onClick={handleAnotherSiteOrder}
            >
              <FormattedMessage id="request.samesite.order" />
            </Button>
          )}
        </Row>
      </div>
    </div>
  );
};

export default OrderSuccessMessage;
