package br.com.sigla.infrastructure.fiscal.xml;

import org.springframework.stereotype.Component;

@Component
public class XmlSerializer {

    public String simpleTag(String tag, String value) {
        return "<" + tag + ">" + value + "</" + tag + ">";
    }
}
