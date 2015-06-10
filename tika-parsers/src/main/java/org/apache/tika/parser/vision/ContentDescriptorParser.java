/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @Author: Aashish Chaudhary (aashish24@gmail.com),
 *          Sangmin Oh
 */
package org.apache.tika.parser.vision;

import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

// Used to talk to web-service
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.HttpMethodParams;

// TODO:
// Error checking
// Testing
// Dealing with multiple images

public class ContentDescriptorParser implements Parser {

  private static final long serialVersionUID = 1L;

  private static final Set<MediaType> SUPPORTED_TYPES =
      Collections.unmodifiableSet(new HashSet<MediaType>(Arrays.asList(
          MediaType.image("jpeg"),
          MediaType.image("png"))));

  public Set<MediaType> getSupportedTypes(ParseContext context) {
    return SUPPORTED_TYPES;
  }

  public void parse(InputStream stream, ContentHandler handler,
      Metadata metadata, ParseContext context)
          throws IOException, SAXException, TikaException {

    String type = metadata.get(Metadata.CONTENT_TYPE);
    if (type != null) {

      TemporaryResources tmp = new TemporaryResources();
      try {
        TikaInputStream tis = TikaInputStream.get(stream, tmp);

        HttpClient client = new HttpClient();

        // Create a method instance. User web service running locally for now
        GetMethod method = new GetMethod("http://localhost:8080/vision?images=" + tis.getFile());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
            new DefaultHttpMethodRetryHandler(3, false));

        try {
          // Execute the method.
          int statusCode = client.executeMethod(method);

          if (statusCode != HttpStatus.SC_OK) {
            System.err.println("Method failed: " + method.getStatusLine());
          }

          // Read the response body.
          // NOTE: We may want to use getResponseBodyAsStream() in the future
          // in case the response is very large
          String response = method.getResponseBodyAsString();

          // NOTE: Currenly our backend is setup for the histogram only
          metadata.set("vision", response);
        } catch (HttpException e) {
          System.err.println("Fatal protocol violation: " + e.getMessage());
          e.printStackTrace();
        } catch (IOException e) {
          System.err.println("Fatal transport error: " + e.getMessage());
          e.printStackTrace();
        } finally {
          // Release the connection.
          method.releaseConnection();
        }

        XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
        xhtml.startDocument();
        xhtml.endDocument();
      } finally {
        tmp.dispose();
      }
    }

    XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
    xhtml.startDocument();
    xhtml.endDocument();
  }

}
