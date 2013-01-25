package com.jme3.gde.welcome.rss;

/**
 *
 * @author Lars Vogel, normenhansen
 */
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.openide.util.Exceptions;

public class RssFeedParser {

    static final String TITLE = "title";
    static final String DESCRIPTION = "description";
    static final String CHANNEL = "channel";
    static final String LANGUAGE = "language";
    static final String COPYRIGHT = "copyright";
    static final String LINK = "link";
    static final String AUTHOR = "author";
    static final String ITEM = "item";
    static final String PUB_DATE = "pubDate";
    static final String GUID = "guid";
    private final URL url;
    private final HTMLEditorKit ekit;
    private final HTMLDocument doc;

    public RssFeedParser(String feedUrl) {
        try {
            this.url = new URL(feedUrl);
            ekit = new HTMLEditorKit();
            doc = new HTMLDocument();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HTMLDocument getDocument() {
        return doc;
    }

    public HTMLEditorKit getEditorKit() {
        return ekit;
    }

    public void updateFeed() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    final Feed feed = readFeed();
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            List<FeedMessage> msgs = feed.getMessages();
                            try {
                                doc.remove(0, doc.getLength());
                                ekit.insertHTML(doc, doc.getLength(),
                                        "<html>"
                                        + "<head>"
                                        + "</head>"
                                        + "<body>",
                                        0,
                                        0,
                                        null);
//                                ekit.insertHTML(doc, doc.getLength(),
//                                        "<h1>"
//                                        + "Latest News"
//                                        + "</h1>",
//                                        0,
//                                        0,
//                                        null);
                                for (FeedMessage feedMessage : msgs) {
                                    ekit.insertHTML(doc, doc.getLength(),
                                            "<h3><a href='"
                                            + feedMessage.getLink()
                                            + "'>"
                                            + feedMessage.getTitle()
                                            + "</a></h3>",
                                            0,
                                            0,
                                            null);
//                                    ekit.insertHTML(doc, doc.getLength(),
//                                            "<p>"
//                                            + feedMessage.getDescription()
//                                            + "</p>",
//                                            0,
//                                            0,
//                                            null);
                                    ekit.insertHTML(doc, doc.getLength(),
                                            "<br/>",
                                            0,
                                            0,
                                            null);
                                }
                                ekit.insertHTML(doc, doc.getLength(),
                                        "</body>"
                                        + "</html>",
                                        0,
                                        0,
                                        null);
                                doc.insertString(0, "", null);
                            } catch (BadLocationException ex) {
                                Exceptions.printStackTrace(ex);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    });
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        t.start();
    }

    @SuppressWarnings("null")
    public Feed readFeed() {
        Feed feed = null;
        try {

            boolean isFeedHeader = true;
            // Set header values intial to the empty string
            String description = "";
            String title = "";
            String link = "";
            String language = "";
            String copyright = "";
            String author = "";
            String pubdate = "";
            String guid = "";

            // First create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // Setup a new eventReader
            InputStream in = read();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
            // Read the XML document
            while (eventReader.hasNext()) {

                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(ITEM)) {
                        if (isFeedHeader) {
                            isFeedHeader = false;
                            feed = new Feed(title, link, description, language,
                                    copyright, pubdate);
                        }
                        event = eventReader.nextEvent();
                        continue;
                    }

                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(TITLE)) {
                        event = eventReader.nextEvent();
                        title = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(DESCRIPTION)) {
                        event = eventReader.nextEvent();
                        if(event.getClass().getName().equals("com.ctc.wstx.evt.CompactStartElement")){
                            description = event.asStartElement().asCharacters().getData();
                        }else{
                            description = event.asCharacters().getData();
                        }
                        continue;
                    }

                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(LINK)) {
                        event = eventReader.nextEvent();
                        //System.out.println("Teh hack: " + event.toString() + event.getClass());
                        Object chars = event.asCharacters();
                        if (chars instanceof javax.xml.stream.events.Characters) {
                            javax.xml.stream.events.Characters jchars = (javax.xml.stream.events.Characters) chars;
                            link = jchars.getData();
                        } else {
                            link = event.asCharacters().getData();
                        }
                        continue;
                    }

                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(GUID)) {
                        event = eventReader.nextEvent();
                        guid = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(LANGUAGE)) {
                        event = eventReader.nextEvent();
                        language = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(AUTHOR)) {
                        event = eventReader.nextEvent();
                        author = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(PUB_DATE)) {
                        event = eventReader.nextEvent();
                        pubdate = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart() != null && event.asStartElement().getName().getLocalPart().equals(COPYRIGHT)) {
                        event = eventReader.nextEvent();
                        copyright = event.asCharacters().getData();
                        continue;
                    }
                } else if (event.isEndElement()) {
                    if (event.asEndElement().getName().getLocalPart() != null && event.asEndElement().getName().getLocalPart().equals(ITEM)) {
                        FeedMessage message = new FeedMessage();
                        message.setAuthor(author);
                        message.setDescription(description);
                        message.setGuid(guid);
                        message.setLink(link);
                        message.setTitle(title);
                        feed.getMessages().add(message);
                        event = eventReader.nextEvent();
                        continue;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return feed;

    }

    private InputStream read() {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
