import React, { useState, useEffect } from "react";
import { FormattedMessage } from "react-intl";
import {
  Heading,
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import PathologyWorkflowTab from "./workflow/PathologyWorkflowTab";

/**
 * PathologyDashboard - Main entry point for pathology laboratory features.
 * Provides access to the pathology workflow notebook and other pathology tools.
 */
const PathologyDashboard = () => {
  const [activeTab, setActiveTab] = useState(0);

  // Pathology notebook ID (created in Liquibase changeset 011-pathology-notebook-instance)
  const pathologyNotebookId = 10;

  return (
    <div className="pathology-dashboard">
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Heading>
            <FormattedMessage id="pathology.dashboard.title" />
          </Heading>
        </Column>
      </Grid>
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <p>
            <FormattedMessage id="pathology.dashboard.description" />
          </p>
        </Column>
      </Grid>

      <Grid>
        <Column lg={16}>
          <Tabs
            selectedIndex={activeTab}
            onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
          >
            <TabList aria-label="Pathology dashboard tabs">
              <Tab>
                <FormattedMessage
                  id="pathology.tab.workflow"
                  defaultMessage="Laboratory Workflow"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="pathology.tab.reports"
                  defaultMessage="Reports"
                />
              </Tab>
            </TabList>

            <TabPanels>
              <TabPanel>
                <PathologyWorkflowTab notebookId={pathologyNotebookId} />
              </TabPanel>
              <TabPanel>
                <p>
                  <FormattedMessage
                    id="pathology.reports.placeholder"
                    defaultMessage="Pathology reports and analytics will be available here."
                  />
                </p>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
};

export default PathologyDashboard;
