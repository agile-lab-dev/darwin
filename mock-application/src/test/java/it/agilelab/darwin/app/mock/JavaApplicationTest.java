package it.agilelab.darwin.app.mock;

import com.typesafe.config.ConfigFactory;
import it.agilelab.darwin.app.mock.classes.OneField;
import it.agilelab.darwin.annotations.AvroSerde;
import it.agilelab.darwin.manager.AvroSchemaManager;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.reflections.Reflections;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class JavaApplicationTest {

    void mainTest() {
        List<Schema> schemas = new ArrayList<>();
        Schema s = ReflectData.get().getSchema(OneField.class);
        schemas.add(s);
        AvroSchemaManager.getSchema(0L);

        AvroSchemaManager.getInstance(ConfigFactory.empty()).registerAll(JavaConversions.asScalaBuffer(schemas));

        long id = AvroSchemaManager.getId(schemas.get(0));
        assert (schemas.get(0) == AvroSchemaManager.getSchema(id));
    }

    void reflectionTest() {
        Reflections reflections = new Reflections("it.agilelab.darwin.app.mock.classes");

        Class<AvroSerde> annotationClass = AvroSerde.class;
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotationClass);
        for (Class c : classes) {
            System.out.println(c.toString());
            try {
                Schema s = ReflectData.get().getSchema(Class.forName(c.getName()));
                System.out.println(s.toString());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
