import React from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { Heading, Grid, Column, Section, Select,SelectItem, FormLabel } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";

const ReportNonConformingEvent = () => {
    const intl = useIntl();

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "report.non.conformity.event", link: "/ReportNonConformingEvent" },
  ];

  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="report.non.conformity.event" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={6}>
            <Select
              labelText={intl.formatMessage({ id: "search.by" })}
              name="unitType"
              id="unitType"
            >
              <SelectItem value="option-1" text="CEDRES" />
              <SelectItem value="option-2" text="CIRBA" />
              <SelectItem value="option-3" text="PROJECT PETROCI" />
            </Select>
          </Column>
          <Column lg={10} />
        </Grid>
      </div>
    </>
  );
};

export default injectIntl(ReportNonConformingEvent);
