package it.agilelab.darwin.app.mock;

import com.typesafe.config.ConfigFactory;
import it.agilelab.darwin.app.mock.classes.OneField;
import it.agilelab.darwin.annotations.AvroSerde;
import it.agilelab.darwin.manager.AvroSchemaManager;
import it.agilelab.darwin.manager.AvroSchemaManagerFactory;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.reflections.Reflections;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class JavaApplicationTest {

    AvroSchemaManager manager = AvroSchemaManagerFactory.getInstance(ConfigFactory.empty());

    void mainTest() {
        List<Schema> schemas = new ArrayList<>();
        Schema s = ReflectData.get().getSchema(OneField.class);
        schemas.add(s);
        manager.getSchema(0L);

        AvroSchemaManagerFactory.getInstance(ConfigFactory.empty()).registerAll(JavaConversions.asScalaBuffer(schemas));

        long id = manager.getId(schemas.get(0));
        assert(manager.getSchema(id).isDefined());
        assert (schemas.get(0) == manager.getSchema(id).get());
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
