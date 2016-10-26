/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.plugins.string.services.string;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UrlUtilityServiceImplTest {

    private static final UrlUtilityServiceImpl service = new UrlUtilityServiceImpl();

    @Test
    public void testGetProtocol(){
        assertEquals(service.getProtocol("http://www.xillio.nl"), "http");
        assertEquals(service.getProtocol("https://www.xillio.nl"), "https");
        assertEquals(service.getProtocol("ftp://xillio.nl"), "ftp");
        assertEquals(service.getProtocol("xyz://xillio.nl"), "xyz");
    }

    @Test
    public void testGetProtocolEmpty(){
        assertEquals(service.getProtocol("xillio.nl"), "");
        assertEquals(service.getProtocol(".nl"), "");
        assertEquals(service.getProtocol(""), "");
        assertEquals(service.getProtocol("//"), "");
        assertEquals(service.getProtocol("://"), "");
    }

    @Test
    public void testGetParentURL(){
        assertEquals(service.getParentUrl("http://www.xillio.nl/info/", ".."), "http://www.xillio.nl/");
        assertEquals(service.getParentUrl("http://www.xillio.nl/info/", "../"), "http://www.xillio.nl/");
        assertEquals(service.getParentUrl("http://www.xillio.nl/info/page1/", "../page2"), "http://www.xillio.nl/info/page2");
        assertEquals(service.getParentUrl("abc://def.ghi.j/h/k/l/m/", "../../../.."), "abc://def.ghi.j/");

        assertEquals(service.getParentUrl("http:/www.example.com/false", ".."), null); // malformed

    }
}