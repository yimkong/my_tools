package dom4j;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.List;

/**
 * author yg
 * description
 * date 2020/5/12
 */
public class Test {
    public static void main(String[] args) throws DocumentException, IOException {
        SAXReader saxReader = new SAXReader();
        File file = new File("test.xml");
        Document document = saxReader.read(file);
        Element rootElement = document.getRootElement();
        List<Element> elements = rootElement.elements();
        for (Element element : elements) {
            Element name = element.element("name");
            if(name.getText().equals("Andy")){
                name.setText("Dan");
                break;
            }else if(name.getText().equals("Dan")) {
                name.setText("Andy");
                break;
            }
        }
        new XMLWriter(new FileOutputStream(file)).write(document);
    }
}
