import React, { useEffect, useMemo, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import {
  ClickableTile,
  Grid,
  Column,
  Search as SearchInput,
} from "@carbon/react";
import { Launch } from "@carbon/icons-react";
import { adminMenuSections, adminLegacyLink } from "./adminMenuConfig";
import { getFromOpenElisServer } from "../utils/Utils";
import config from "../../config.json";

const matchesSearch = (text, query) => {
  if (!query) return true;
  return text.toLowerCase().includes(query.toLowerCase());
};

function AdminCard({ icon: Icon, label, dataCy, onClick, external }) {
  return (
    <ClickableTile
      onClick={onClick}
      data-cy={dataCy}
      className="admin-card"
      aria-label={label}
    >
      <div className="admin-card-icon">{Icon ? <Icon size={28} /> : null}</div>
      <div className="admin-card-label">
        <span>{label}</span>
        {external && (
          <span
            className="admin-card-external"
            aria-label="Opens in new window"
          >
            <Launch size={14} />
          </span>
        )}
      </div>
    </ClickableTile>
  );
}

export default function AdminLandingPage() {
  const intl = useIntl();
  const history = useHistory();
  const [isTrainingInstallation, setIsTrainingInstallation] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    getFromOpenElisServer("/rest/database-cleaning/status", (response) => {
      if (response) {
        setIsTrainingInstallation(!!response.trainingInstallation);
      }
    });
  }, []);

  const sections = useMemo(() => {
    return adminMenuSections
      .map((section) => {
        const items = section.items
          .filter((item) =>
            item.requiresTrainingInstallation ? isTrainingInstallation : true,
          )
          .filter((item) => {
            const label = item.defaultLabel
              ? intl.formatMessage({
                  id: item.labelKey,
                  defaultMessage: item.defaultLabel,
                })
              : intl.formatMessage({ id: item.labelKey });
            return matchesSearch(label, searchQuery);
          });
        return { ...section, items };
      })
      .filter((section) => section.items.length > 0);
  }, [intl, isTrainingInstallation, searchQuery]);

  const legacyLabel = intl.formatMessage({ id: adminLegacyLink.labelKey });
  const showLegacy = matchesSearch(legacyLabel, searchQuery);

  const renderItemLabel = (item) =>
    item.defaultLabel ? (
      <FormattedMessage id={item.labelKey} defaultMessage={item.defaultLabel} />
    ) : (
      <FormattedMessage id={item.labelKey} />
    );

  const navigate = (path) => () => history.push(path);
  const openExternal = (href) => () =>
    window.open(config.serverBaseUrl + href, "_blank", "noopener,noreferrer");

  return (
    <div className="admin-landing">
      <header className="admin-landing-header">
        <h1>
          <FormattedMessage
            id="admin.landing.title"
            defaultMessage="Admin Settings"
          />
        </h1>
        <p>
          <FormattedMessage
            id="admin.landing.subtitle"
            defaultMessage="Configure laboratory operations, users, and system behavior."
          />
        </p>
        <div className="admin-landing-search">
          <SearchInput
            size="lg"
            labelText=""
            placeholder={intl.formatMessage({
              id: "admin.landing.searchPlaceholder",
              defaultMessage: "Search settings…",
            })}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onClear={() => setSearchQuery("")}
          />
        </div>
      </header>

      {sections.map((section) => (
        <section key={section.id} className="admin-landing-section">
          <h2 className="admin-landing-section-title">
            <FormattedMessage
              id={section.titleKey}
              defaultMessage={section.defaultTitle}
            />
          </h2>
          <Grid narrow className="admin-landing-grid">
            {section.items.map((item) => (
              <Column
                sm={4}
                md={4}
                lg={4}
                xlg={4}
                key={item.id}
                className="admin-landing-column"
              >
                <AdminCard
                  icon={item.icon}
                  label={renderItemLabel(item)}
                  dataCy={item.dataCy || item.id}
                  onClick={navigate(item.path)}
                />
              </Column>
            ))}
          </Grid>
        </section>
      ))}

      {showLegacy && (
        <section className="admin-landing-section">
          <h2 className="admin-landing-section-title">
            <FormattedMessage
              id="admin.section.legacy"
              defaultMessage="Legacy Tools"
            />
          </h2>
          <Grid narrow className="admin-landing-grid">
            <Column
              sm={4}
              md={4}
              lg={4}
              xlg={4}
              className="admin-landing-column"
            >
              <AdminCard
                icon={adminLegacyLink.icon}
                label={<FormattedMessage id={adminLegacyLink.labelKey} />}
                dataCy={adminLegacyLink.id}
                onClick={openExternal(adminLegacyLink.href)}
                external
              />
            </Column>
          </Grid>
        </section>
      )}

      {sections.length === 0 && !showLegacy && (
        <div className="admin-landing-empty">
          <FormattedMessage
            id="admin.landing.noResults"
            defaultMessage="No settings match your search."
          />
        </div>
      )}
    </div>
  );
}
