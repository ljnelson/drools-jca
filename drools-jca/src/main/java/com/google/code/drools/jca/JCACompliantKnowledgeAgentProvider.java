/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * $Id$
 *
 * Copyright (c) 2010 Laird Nelson.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.google.code.drools.jca;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;

import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentConfiguration;

import org.drools.agent.impl.KnowledgeAgentProviderImpl;

import org.drools.builder.KnowledgeBuilderConfiguration;

public class JCACompliantKnowledgeAgentProvider extends KnowledgeAgentProviderImpl {

  public KnowledgeAgent newKnowledgeAgent(final String name) {
    return this.newKnowledgeAgent(name, KnowledgeBaseFactory.newKnowledgeBase());
  }

  @Override
  public KnowledgeAgent newKnowledgeAgent(final String name, final KnowledgeBase kbase) {
    return this.newKnowledgeAgent(name, kbase, this.newKnowledgeAgentConfiguration(), null);
  }

  @Override
  public KnowledgeAgent newKnowledgeAgent(final String name, final KnowledgeBase kbase, final KnowledgeAgentConfiguration configuration) {
    return this.newKnowledgeAgent(name, kbase, configuration, null);
  }

  @Override
  public KnowledgeAgent newKnowledgeAgent(final String name, final KnowledgeBase kbase, final KnowledgeAgentConfiguration configuration, final KnowledgeBuilderConfiguration anotherConfiguration) {
    return new JCACompliantKnowledgeAgent(name, kbase, configuration, anotherConfiguration);
  }

}