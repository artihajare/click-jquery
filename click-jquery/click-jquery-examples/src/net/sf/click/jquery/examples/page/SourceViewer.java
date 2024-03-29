/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.click.jquery.examples.page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.click.Page;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.HtmlStringBuffer;

import org.apache.commons.lang.StringUtils;

/**
 * Provides a Java source code, HTML and XML examples rendering page.
 */
public class SourceViewer extends BorderPage {

	private static final long serialVersionUID = 1L;

    private static final String[] HTML_KEYWORDS = { "html", "head", "style",
            "script", "title", "link", "body", "h1", "h2", "h3", "h4", "h5",
            "h6", "p", "hr", "br", "span", "table", "tr", "th", "td", "a", "b",
            "i", "u", "ul", "ol", "li", "form", "div", "input", "fieldset",
            "pre", "tt", "ajax-response", "response", "%@", "%@taglib",
            "jsp:include", "c:forEach", "c:choose", "c:when", "c:otherwise",
            "fmt:formatNumber", "fmt:formatDate", "center" };

    private static final String[] XML_KEYWORDS = { "click-app", "pages",
            "page", "excludes", "headers", "header", "format", "mode", "type",
            "filter-name", "filter-class", "filter-mapping", "filter",
            "web-app", "display-name", "description", "servlet-mapping",
            "servlet-name", "servlet-class", "init-param", "param-name",
            "param-value", "servlet", "load-on-startup", "security-constraint",
            "web-resource-collection", "auth-constraint", "role-name",
            "login-config", "auth-method", "realm-name", "security-role",
            "url-pattern", "welcome-file-list", "welcome-file", "Context",
            "ResourceLink", "menu", "?xml", "controls", "control",
            "listener-class", "listener" };

    private static final String[] VELOCITY_KEYWORDS = { "#if", "#if(",
            "#elseif", "#elseif(", "#else", "#else(", "#end", "#set", "#set(",
            "#include", "#include(", "#parse", "#parse(", "#stop", "#macro",
            "#macro(", "#foreach", "#foreach(", "##", "#*", "*#", "#" };

    private boolean isJava = false;

    private boolean isXml = false;

    private boolean isHtml = false;

    /**
     * @see Page#onGet()
     */
    public void onGet() {
        HttpServletRequest request = getContext().getRequest();

        String filename = request.getParameter("filename");

        if (filename != null) {
            loadFilename(filename);

        } else {
            addModel("error", "filename not defined");
        }
    }

    private void loadFilename(String filename) {
        ServletContext context = getContext().getServletContext();

        // Orion server requires '/' prefix to find resources
        String resourceFilename =
            (filename.charAt(0) != '/') ? "/" + filename : filename;

        InputStream in = null;
        try {
            in = context.getResourceAsStream(resourceFilename);

            if (in == null && filename.endsWith(".htm")) {
                resourceFilename =
                    resourceFilename.substring(0, resourceFilename.length() - 4)
                    + ".jsp";

                in = context.getResourceAsStream(resourceFilename);
            }

            if (in != null) {

                loadResource(in, filename);

            } else {
                addModel("error", "File " + resourceFilename + " not found");
            }

        } catch (IOException e) {
            addModel("error", "Could not read " + resourceFilename);

        } finally {
            ClickUtils.close(in);
        }
    }

    private void loadResource(InputStream inputStream, String name)
            throws IOException {

        isJava = name.endsWith(".java");
        isXml = name.endsWith(".xml");
        isHtml = name.endsWith(".htm");
        if (!isHtml) {
            isHtml = name.endsWith(".html");
        }
        if (!isHtml) {
            isHtml = name.endsWith(".vm");
        }
        if (!isHtml) {
            isHtml = name.endsWith(".jsp");
        }

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream));

        HtmlStringBuffer buffer = new HtmlStringBuffer();

        String line = reader.readLine();

        while (line != null) {
            buffer.append(getEncodedLine(line));
            buffer.append("\n");
            line = reader.readLine();
        }

        if (isHtml) {
            addModel("templateSource", buffer.toString());
        } else {
            addModel("source", buffer.toString());
        }

        addModel("name", name);
    }

    private String getEncodedLine(String line) {

        if (isHtml) {
            line = ClickUtils.escapeHtml(line);

            for (int i = 0; i < HTML_KEYWORDS.length; i++) {
                String keyword = HTML_KEYWORDS[i];
                line = renderHtmlKeywords(line, keyword);
            }

            for (int i = 0; i < VELOCITY_KEYWORDS.length; i++) {
                String keyword = VELOCITY_KEYWORDS[i];
                line = renderVelocityKeywords(line, keyword);
            }

            String renderedDollar = "<font color=\"red\">$</font>";

            line = StringUtils.replace(line, "$", renderedDollar);

        } else if (isXml) {
            line = ClickUtils.escapeHtml(line);

            for (int i = 0; i < XML_KEYWORDS.length; i++) {
                String keyword = XML_KEYWORDS[i];
                line = renderXmlKeywords(line, keyword);
            }

        } else {
            line = ClickUtils.escapeHtml(line);
        }

        return line;
    }

    private String renderVelocityKeywords(String line, String token) {
        String markupToken = renderVelocityToken(token);

        line = StringUtils.replace
            (line, " " + token + " ", " " + markupToken + " ");

        if (line.startsWith(token)) {
            line = markupToken + line.substring(token.length());
        }

        if (line.endsWith(token)) {
            line = line.substring(0, line.length() - token.length())
                    + markupToken;
        }

        return line;
    }

    private String renderHtmlKeywords(String line, String token) {

        String markupToken = "&lt;" + token + "&gt;";
        String renderedToken = "&lt;" + renderHtmlToken(token) + "&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;" + token + "/&gt;";
        renderedToken = "&lt;" + renderHtmlToken(token) + "/&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;/" + token + "&gt;";
        renderedToken = "&lt;/" + renderHtmlToken(token) + "&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;" + token + " ";
        renderedToken = "&lt;" + renderHtmlToken(token) + " ";
        line = StringUtils.replace(line, markupToken, renderedToken);

        return line;
    }

    private String renderXmlKeywords(String line, String token) {

        String markupToken = "&lt;" + token + "&gt;";
        String renderedToken = "&lt;" + renderXmlToken(token) + "&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;" + token + "/&gt;";
        renderedToken = "&lt;" + renderXmlToken(token) + "/&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;/" + token + "&gt;";
        renderedToken = "&lt;/" + renderXmlToken(token) + "&gt;";
        line = StringUtils.replace(line, markupToken, renderedToken);

        markupToken = "&lt;" + token + " ";
        renderedToken = "&lt;" + renderXmlToken(token) + " ";
        line = StringUtils.replace(line, markupToken, renderedToken);

        return line;
    }

    private String renderHtmlToken(String token) {
        return "<font color=\"#00029F\">" + token + "</font>";
    }

    private String renderXmlToken(String token) {
        return "<font color=\"#00029F\">" + token + "</font>";
    }

    private String renderVelocityToken(String token) {
        return "<font color=\"red\">" + token + "</font>";
    }
    
}
