/**
 * PAGE_TYPE_REGISTRY - Maps pageType strings (stored in DB) to React page components.
 *
 * To add a new lab:
 *   1. Add a JSON file in volume/configuration/backend/notebook-templates/<lab>.json
 *      with pages declaring pageType, manifestColumns, and columns.
 *   2. For steps that are identical across labs (reception, QC, storage, reporting,
 *      disposal, environmental monitoring), use a generic_* pageType — no new React
 *      component is needed.
 *   3. For truly lab-specific steps (e.g. TB culture reading, Pathology cassettes),
 *      write a component and register it below.
 *
 * Generic pageTypes (config-driven, no per-lab code):
 *   generic_sample_reception      — manifest import + sample grid (columns from JSON)
 *   generic_quality_check         — QC accept/reject workflow
 *   generic_sample_processing     — processing step log
 *   generic_storage               — storage location assignment
 *   generic_disposal              — disposal / archiving
 *   generic_reporting             — metrics and date-range reporting
 *   generic_environmental_monitor — temperature and environment logging
 *
 * No WorkflowTab file is needed per lab. GenericWorkflowTab handles all labs.
 */
import React from "react";

// Bacteriology
import {
  BacteriologyIsolateCreationPage,
  BacteriologyProcessingQCPage,
  BacteriologyAssayTestExecutionPage,
  BacteriologyPostAnalysisPage,
  BacteriologySampleRetrievalDisposalPage,
  BacteriologyReportingDataExportPage,
} from "../pages/bacteriology";

// MNTD
import {
  MNTDSampleProcessingPage,
  MNTDAliquotingPage,
  MNTDProcessingQCPage,
  MNTDTestAssignmentPage,
  MNTDTestExecutionPage,
  MNTDSampleArchivingPage,
  MNTDDataAnalysisPage,
} from "../pages/mntd";

// Pharmaceutical
import {
  PharmaceuticalProcessingPage,
  PharmaceuticalTestingPage,
  PharmaceuticalReportingPage,
  PharmaceuticalDisposalPage,
} from "../pages/pharma";

// Traditional Medicine
import {
  TraditionalMedicineAuthenticationPage,
  TraditionalMedicinePreparationPage,
  TraditionalMedicineExtractionPage,
  TraditionalMedicineAnalyticalPage,
  TraditionalMedicineTestingPage,
  TraditionalMedicineFormulationPage,
} from "../pages/traditionalmedicine";

// Tuberculosis
import {
  TBInitialProcessingPage,
  TBIncubationMonitoringPage,
  TBTestExecutionPage,
  TBDisposalArchivingPage,
  TBReportingPage,
} from "../pages/tb";

// Pathology
import {
  PathologyQualityControlPage,
  PathologyGrossExaminationPage,
  PathologyCassettesPage,
  PathologyBlocksPage,
  PathologySlidesPage,
  PathologyStainingPage,
  PathologyTestingMicroscopyPage,
  PathologyReportingPage,
  PathologyDisposalArchivingPage,
} from "../pages/pathology";

// Bioanalytical
import BioanalyticalTestAssignmentPage from "../pages/bioanalytical/BioanalyticalTestAssignmentPage";
import BioanalyticalAnalyticalExecutionPage from "../pages/bioanalytical/BioanalyticalAnalyticalExecutionPage";
import BioanalyticalReportingPage from "../pages/bioanalytical/BioanalyticalReportingPage";
// Bioequivalence
import BioequivalenceTestAssignmentPage from "../pages/bioequivalence/BioequivalenceTestAssignmentPage";
import BioequivalenceAnalyticalExecutionPage from "../pages/bioequivalence/BioequivalenceAnalyticalExecutionPage";
import BioequivalenceReportingPage from "../pages/bioequivalence/BioequivalenceReportingPage";

// MedLab
import PatientOrderEntryPage from "../pages/PatientOrderEntryPage";
import SampleCollectionPage from "../pages/SampleCollectionPage";
import MedLabQualityCheckPage from "../pages/medlab/MedLabQualityCheckPage";
import MedLabSampleRoutingPage from "../pages/medlab/MedLabSampleRoutingPage";
import MedLabSampleProcessingPage from "../pages/medlab/MedLabSampleProcessingPage";
import TestingAnalyzerPage from "../pages/TestingAnalyzerPage";
import ResultEntryPage from "../pages/ResultEntryPage";
import ValidationReportingPage from "../pages/ValidationReportingPage";
import EndOfProjectArchivingPage from "../pages/EndOfProjectArchivingPage";

// Biorepository
import {
  BiorepositoryEnvironmentalMonitoringPage,
  BiorepositoryRetentionDisposalPage,
  BiorepositorySampleRequestPage,
  BiorepositoryQCInspectionPage,
  BiorepositoryReportingPage,
} from "../pages/biorepository";

// GBD
import {
  GBDDNARNAExtractionPage,
  GBDQualityQuantityAssessmentPage,
  GBDPCRAmplificationPage,
  GBDGelElectrophoresesPage,
  GBDLibraryPreparationPage,
  GBDBioanalyzerQCPage,
  GBDSequencingPage,
  GBDBioinformaticsAnalysisPage,
} from "../pages/gbd";

// Immunology
import ImmunologyInitialProcessingPage from "../pages/immunology/ImmunologyInitialProcessingPage";
import ImmunologyAdditionalAssaysPage from "../pages/immunology/ImmunologyAdditionalAssaysPage";
import ImmunologyChildSampleCreationPage from "../pages/immunology/ImmunologyChildSampleCreationPage";
import ImmunologyPostAnalysisPage from "../pages/immunology/ImmunologyPostAnalysisPage";
import ImmunologyResultCompilationPage from "../pages/immunology/ImmunologyResultCompilationPage";
import ImmunologyArchivingPage from "../pages/immunology/ImmunologyArchivingPage";
import ImmunologyDataAnalysisPage from "../pages/immunology/ImmunologyDataAnalysisPage";
import PrepPage from "../pages/PrepPage";
import AnalysisPage from "../pages/AnalysisPage";
import SampleRoutingPage from "../pages/SampleRoutingPage";

// Virology
import VirologyMediaPreparationPage from "../pages/virology/VirologyMediaPreparationPage";
import VirologyCellCulturePage from "../pages/virology/VirologyCellCulturePage";
import VirologyQualityControlPage from "../pages/virology/VirologyQualityControlPage";
import VirologyVirusCulturePage from "../pages/virology/VirologyVirusCulturePage";
import VirologyDarkRoomImagingPage from "../pages/virology/VirologyDarkRoomImagingPage";
import VirologyFormulationPage from "../pages/virology/VirologyFormulationPage";
import VirologyFeedingPage from "../pages/virology/VirologyFeedingPage";
import VirologyPackagingPage from "../pages/virology/VirologyPackagingPage";
import VirologyVirusIsolationPage from "../pages/virology/VirologyVirusIsolationPage";
import VirologyTiterMeasurementPage from "../pages/virology/VirologyTiterMeasurementPage";
import VirologyGenomeSequencingPage from "../pages/virology/VirologyGenomeSequencingPage";
import VirologySeedVirusProductionPage from "../pages/virology/VirologySeedVirusProductionPage";
import VirologyTrialsPage from "../pages/virology/VirologyTrialsPage";

// Generic pages — shared across all labs that use generic_* pageTypes
import SampleReceptionPage from "../pages/SampleReceptionPage";
import GenericQualityCheckPage from "../pages/GenericQualityCheckPage";
import GenericStoragePage from "../pages/GenericStoragePage";
import SampleProcessingPage from "../pages/SampleProcessingPage";
import ReportingPage from "../pages/ReportingPage";
import SampleStoragePage from "../pages/SampleStoragePage";

/**
 * Immunology Child Samples + Routing — combined view for page order 4.
 * Child sample creation and destination routing are shown together on one page.
 */
function ImmunologyChildSamplesWithRoutingPage(props) {
  return (
    <React.Fragment>
      <ImmunologyChildSampleCreationPage {...props} />
      <div className="routing-section" style={{ marginTop: "2rem" }}>
        <SampleRoutingPage {...props} />
      </div>
    </React.Fragment>
  );
}

/**
 * Registry mapping pageType string → React component.
 * Keys must match the pageType values stored in the notebook_page table.
 */
const PAGE_TYPE_REGISTRY = {
  // Bacteriology
  bacteriology_reception_verification: GenericQualityCheckPage,
  bacteriology_isolate_creation: BacteriologyIsolateCreationPage,
  bacteriology_temporary_storage: GenericStoragePage,
  bacteriology_processing_qc: BacteriologyProcessingQCPage,
  bacteriology_assay_execution: BacteriologyAssayTestExecutionPage,
  bacteriology_post_analysis: BacteriologyPostAnalysisPage,
  bacteriology_retrieval_disposal: BacteriologySampleRetrievalDisposalPage,
  bacteriology_reporting: BacteriologyReportingDataExportPage,

  // MNTD
  mntd_reception_verification: GenericQualityCheckPage,
  mntd_temporary_storage: GenericStoragePage,
  mntd_sample_processing: MNTDSampleProcessingPage,
  mntd_aliquoting: MNTDAliquotingPage,
  mntd_processing_qc: MNTDProcessingQCPage,
  mntd_test_assignment: MNTDTestAssignmentPage,
  mntd_test_execution: MNTDTestExecutionPage,
  mntd_archiving: MNTDSampleArchivingPage,
  mntd_data_analysis: MNTDDataAnalysisPage,

  // Pharmaceutical
  pharma_quality_check: GenericQualityCheckPage,
  pharma_processing: PharmaceuticalProcessingPage,
  pharma_testing: PharmaceuticalTestingPage,
  pharma_storage: GenericStoragePage,
  pharma_reporting: PharmaceuticalReportingPage,
  pharma_disposal: PharmaceuticalDisposalPage,

  // Traditional Medicine
  trad_med_authentication: TraditionalMedicineAuthenticationPage,
  trad_med_auth_storage: GenericStoragePage,
  trad_med_preparation: TraditionalMedicinePreparationPage,
  trad_med_extraction: TraditionalMedicineExtractionPage,
  trad_med_analytical: TraditionalMedicineAnalyticalPage,
  trad_med_testing: TraditionalMedicineTestingPage,
  trad_med_formulation: TraditionalMedicineFormulationPage,

  // Tuberculosis
  tb_quality_check: GenericQualityCheckPage,
  tb_initial_processing: TBInitialProcessingPage,
  tb_incubation_monitoring: TBIncubationMonitoringPage,
  tb_test_execution: TBTestExecutionPage,
  tb_storage_assignment: GenericStoragePage,
  tb_disposal_archiving: TBDisposalArchivingPage,
  tb_reporting: TBReportingPage,

  // Pathology
  pathology_quality_control: PathologyQualityControlPage,
  pathology_gross_examination: PathologyGrossExaminationPage,
  pathology_cassettes: PathologyCassettesPage,
  pathology_blocks: PathologyBlocksPage,
  pathology_slides: PathologySlidesPage,
  pathology_staining: PathologyStainingPage,
  pathology_testing_microscopy: PathologyTestingMicroscopyPage,
  pathology_storage_inventory: GenericStoragePage,
  pathology_reporting: PathologyReportingPage,
  pathology_disposal_archiving: PathologyDisposalArchivingPage,

  // Bioanalytical
  bioanalytical_test_assignment: BioanalyticalTestAssignmentPage,
  bioanalytical_execution: BioanalyticalAnalyticalExecutionPage,
  bioanalytical_reporting: BioanalyticalReportingPage,
  bioanalytical_storage: GenericStoragePage,

  // Bioequivalence
  bioeq_test_assignment: BioequivalenceTestAssignmentPage,
  bioeq_execution: BioequivalenceAnalyticalExecutionPage,
  bioeq_reporting: BioequivalenceReportingPage,
  bioeq_storage: GenericStoragePage,

  // MedLab
  medlab_patient_order: PatientOrderEntryPage,
  medlab_sample_collection: SampleCollectionPage,
  medlab_quality_check: MedLabQualityCheckPage,
  medlab_sample_routing: MedLabSampleRoutingPage,
  medlab_sample_processing: MedLabSampleProcessingPage,
  medlab_testing_analyzer: TestingAnalyzerPage,
  medlab_result_entry: ResultEntryPage,
  medlab_validation_reporting: ValidationReportingPage,
  medlab_archiving: EndOfProjectArchivingPage,

  // Biorepository
  biorepo_storage_assignment: GenericStoragePage,
  biorepo_environmental_monitoring: BiorepositoryEnvironmentalMonitoringPage,
  biorepo_retention_disposal: BiorepositoryRetentionDisposalPage,
  biorepo_sample_request: BiorepositorySampleRequestPage,
  biorepo_qc_inspection: BiorepositoryQCInspectionPage,
  biorepo_reporting: BiorepositoryReportingPage,

  // GBD
  gbd_dna_rna_extraction: GBDDNARNAExtractionPage,
  gbd_quality_quantity_assessment: GBDQualityQuantityAssessmentPage,
  gbd_pcr_amplification: GBDPCRAmplificationPage,
  gbd_gel_electrophoresis: GBDGelElectrophoresesPage,
  gbd_library_preparation: GBDLibraryPreparationPage,
  gbd_bioanalyzer_qc: GBDBioanalyzerQCPage,
  gbd_sequencing: GBDSequencingPage,
  gbd_bioinformatics: GBDBioinformaticsAnalysisPage,
  gbd_storage_monitoring: GenericStoragePage,

  // Immunology
  immunology_initial_processing: ImmunologyInitialProcessingPage,
  immunology_additional_assays: ImmunologyAdditionalAssaysPage,
  immunology_child_samples: ImmunologyChildSamplesWithRoutingPage,
  immunology_prep: PrepPage,
  immunology_analysis: AnalysisPage,
  immunology_post_analysis: ImmunologyPostAnalysisPage,
  immunology_result_compilation: ImmunologyResultCompilationPage,
  immunology_archiving: ImmunologyArchivingPage,
  immunology_data_analysis: ImmunologyDataAnalysisPage,

  // Virology
  virology_media_preparation: VirologyMediaPreparationPage,
  virology_cell_culture: VirologyCellCulturePage,
  virology_quality_control: VirologyQualityControlPage,
  virology_virus_culture: VirologyVirusCulturePage,
  virology_dark_room_imaging: VirologyDarkRoomImagingPage,
  virology_formulation: VirologyFormulationPage,
  virology_feeding: VirologyFeedingPage,
  virology_packaging: VirologyPackagingPage,
  virology_virus_isolation: VirologyVirusIsolationPage,
  virology_titer_measurement: VirologyTiterMeasurementPage,
  virology_genome_sequencing: VirologyGenomeSequencingPage,
  virology_seed_virus_production: VirologySeedVirusProductionPage,
  virology_trials: VirologyTrialsPage,

  // ── Generic pages — config-driven, no per-lab React code needed ─────────────
  // Labs declare these pageTypes in JSON and supply manifestColumns / columns.
  generic_sample_reception: SampleReceptionPage,
  generic_quality_check: GenericQualityCheckPage,
  generic_sample_processing: SampleProcessingPage,
  generic_storage: GenericStoragePage,
  generic_sample_storage: SampleStoragePage,
  generic_disposal: EndOfProjectArchivingPage,
  generic_reporting: ReportingPage,
};

export default PAGE_TYPE_REGISTRY;
