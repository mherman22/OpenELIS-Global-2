package org.openelisglobal.notebook.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NoteBookOrmTest {

    @Test
    public void questionnaireUuid_fieldMapsToCorrectColumn() throws NoSuchFieldException {
        Field field = NoteBook.class.getDeclaredField("questionnaireUuid");
        Column col = field.getAnnotation(Column.class);
        assertNotNull("@Column missing on questionnaireUuid", col);
        assertEquals("questionnaire_uuid", col.name());
    }

    @Test
    public void questionnaireUuid_fieldIsStringType() throws NoSuchFieldException {
        Field field = NoteBook.class.getDeclaredField("questionnaireUuid");
        assertEquals(String.class, field.getType());
    }
}
