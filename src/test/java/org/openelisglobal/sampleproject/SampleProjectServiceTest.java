package org.openelisglobal.sampleproject;

import static org.junit.Assert.*;

import java.sql.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleProjectServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleProjectService sampleProjectService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/sampleproject.xml");
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnCorrectSampleProject() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("1");

        assertNotNull(sampleProject);
        assertNotNull(sampleProject.getSample());
        assertNotNull(sampleProject.getProject());
        assertEquals("1", sampleProject.getSample().getId());
        assertEquals("1", sampleProject.getProject().getId());
        assertEquals("Y", sampleProject.getIsPermanent());
    }

    @Test
    public void getData_shouldPopulateSampleProject() {
        SampleProject emptyProject = new SampleProject();
        emptyProject.setId("1");

        sampleProjectService.getData(emptyProject);

        assertNotNull(emptyProject.getProject());
        assertEquals("1", emptyProject.getProject().getId());
    }

    @Test
    public void getData_shouldHandleNonExistentId() {
        SampleProject nonExistent = new SampleProject();
        nonExistent.setId("999");

        sampleProjectService.getData(nonExistent);

        assertNull(nonExistent.getId());
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnNullForNullInput() {
        SampleProject result = sampleProjectService.getSampleProjectBySampleId(null);
        assertNull(result);
    }

    @Test
    public void getSampleProjectBySampleId_shouldReturnNullForNonExistentSample() {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId("999");
        assertNull(sampleProject);
    }

    @Test
    public void updateSampleProject_shouldModifyExistingSampleProject() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertEquals("Y", existingProject.getIsPermanent());

        existingProject.setIsPermanent("N");
        sampleProjectService.save(existingProject);

        SampleProject updatedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertEquals("N", updatedProject.getIsPermanent());
    }

    @Test
    public void getByOrganizationProjectAndReceivedOnRange_shouldReturnEmptyListForInvalidRange() {
        Date lowDate = Date.valueOf("2024-01-01");
        Date highDate = Date.valueOf("2024-02-01");
        List<SampleProject> projects = sampleProjectService.getByOrganizationProjectAndReceivedOnRange("1",
                "Test Project", lowDate, highDate);
        assertEquals("No projects should match a date range outside test data", 0, projects.size());
    }

    @Test
    public void updateSampleProject_shouldHandleNullValuesGracefully() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        existingProject.setIsPermanent(null);
        sampleProjectService.save(existingProject);

        SampleProject updatedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNull(updatedProject.getIsPermanent());
    }

    @Test
    public void getAllSampleProjects_shouldReturnNonEmptyList() {
        List<SampleProject> projects = sampleProjectService.getAll();
        assertNotNull(projects);
        assertTrue("Test data contains at least 1 sample project", projects.size() >= 1);
        assertEquals("1", projects.get(0).getId());
    }

    @Test
    public void deleteSampleProject_shouldRemoveProject() {
        SampleProject existingProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNotNull(existingProject);

        sampleProjectService.delete(existingProject);

        SampleProject deletedProject = sampleProjectService.getSampleProjectBySampleId("1");
        assertNull(deletedProject);
    }

    // --- Negative / edge-case tests ---

    @Test
    public void getSampleProjectBySampleId_shouldReturnNullForEmptyString() {
        SampleProject result = sampleProjectService.getSampleProjectBySampleId("");
        assertNull(result);
    }

    @Test
    public void getByOrganizationProjectAndReceivedOnRange_shouldReturnEmptyForNonExistentOrg() {
        Date lowDate = Date.valueOf("2023-01-01");
        Date highDate = Date.valueOf("2025-12-31");
        List<SampleProject> projects = sampleProjectService.getByOrganizationProjectAndReceivedOnRange("999",
                "Test Project", lowDate, highDate);
        assertEquals("Non-existent org should return empty list", 0, projects.size());
    }

    @Test
    public void getByOrganizationProjectAndReceivedOnRange_shouldReturnEmptyForReversedDateRange() {
        Date lowDate = Date.valueOf("2025-01-01");
        Date highDate = Date.valueOf("2020-01-01");
        List<SampleProject> projects = sampleProjectService.getByOrganizationProjectAndReceivedOnRange("1",
                "Test Project", lowDate, highDate);
        assertEquals("Reversed date range should return empty list", 0, projects.size());
    }
}
