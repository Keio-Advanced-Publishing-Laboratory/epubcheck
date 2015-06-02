/*
 * Copyright (c) 2011 Adobe Systems Incorporated
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in
 *  the Software without restriction, including without limitation the rights to
 *  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.adobe.epubcheck.nav;

import java.util.Set;

import com.adobe.epubcheck.api.EPUBProfile;
import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.messages.MessageLocation;
import com.adobe.epubcheck.opf.ContentChecker;
import com.adobe.epubcheck.opf.DocumentValidator;
import com.adobe.epubcheck.opf.OPFData;
import com.adobe.epubcheck.opf.ValidationContext;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.vocab.EpubCheckVocab;
import com.adobe.epubcheck.vocab.VocabUtil;
import com.adobe.epubcheck.xml.XMLHandler;
import com.adobe.epubcheck.xml.XMLParser;
import com.adobe.epubcheck.xml.XMLValidators;
import com.google.common.base.Preconditions;

public class NavChecker implements ContentChecker, DocumentValidator
{
  private final ValidationContext context;
  private final Report report;
  private final String path;
  private final Set<EpubCheckVocab.PROPERTIES> customProperties;

  public NavChecker(ValidationContext context)
  {
    Preconditions.checkState("application/xhtml+xml".equals(context.mimeType));
    this.context = context;
    this.report = context.report;
    this.path = context.path;
    if (context.version == EPUBVersion.VERSION_2)
    {
      context.report.message(MessageId.NAV_001, new MessageLocation(path, 0, 0));
    }

    // Parse EpubCheck custom properties
    // These properties are "fake" temporary properties appended to the
    // 'properties' field to store info needed by EpubCheck (e.g. whether the
    // document being tested is a linear primary item).
    this.customProperties = VocabUtil.parsePropertyListAsEnumSet(context.properties,
        EpubCheckVocab.VOCAB_MAP, EpubCheckVocab.PROPERTIES.class);
  }

  @Override
  public void runChecks()
  {
    if (!context.ocf.get().hasEntry(path))
    {
      report.message(MessageId.RSC_001, new MessageLocation(context.ocf.get().getName(), -1, -1),
          path);
    }
    else if (!context.ocf.get().canDecrypt(path))
    {
      report.message(MessageId.RSC_004, new MessageLocation(context.ocf.get().getName(), 0, 0),
          path);
    }
    else
    {
      validate();
    }
  }

  public boolean validate()
  {
    int fatalErrors = report.getFatalErrorCount();
    int errors = report.getErrorCount();
    int warnings = report.getWarningCount();
    XMLParser navParser = new XMLParser(context);

    XMLHandler navHandler = new NavHandler(context, navParser);
    navParser.addXMLHandler(navHandler);
    navParser.addValidator(XMLValidators.NAV_30_RNC.get());
    navParser.addValidator(XMLValidators.XHTML_30_SCH.get());
    navParser.addValidator(XMLValidators.NAV_30_SCH.get());
    if (!customProperties.contains(EpubCheckVocab.PROPERTIES.NON_LINEAR)
        && (context.profile == EPUBProfile.EDUPUB || context.pubTypes
            .contains(OPFData.DC_TYPE_EDUPUB)))
    {
      navParser.addValidator(XMLValidators.XHTML_EDUPUB_STRUCTURE_SCH.get());
      navParser.addValidator(XMLValidators.XHTML_EDUPUB_SEMANTICS_SCH.get());
    }
    navParser.process();

    return ((fatalErrors == report.getFatalErrorCount()) && (errors == report.getErrorCount()) && (warnings == report
        .getWarningCount()));
  }
}
