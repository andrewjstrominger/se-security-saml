/* Copyright 2009 Vladimir Sch�fer
 *
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
package org.springframework.security.saml.websso;

import static org.easymock.EasyMock.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.security.credential.CredentialResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.CredentialsExpiredException;
import org.springframework.security.saml.SAMLTestBase;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.security.saml.storage.SAMLMessageStorage;

/**
 * @author Vladimir Sch�fer
 */
public class WebSSOProfileConsumerImplTest extends SAMLTestBase {

    ApplicationContext context;
    WebSSOProfileConsumerImpl profile;
    SAMLMessageStorage storage;
    BasicSAMLMessageContext messageContext;
    MetadataManager manager;
    XMLObjectBuilderFactory builderFactory;
    WebSSOProfileTestHelper helper;
    CredentialResolver resolver;

    @Before
    public void initialize() throws Exception {
        String resName = "/" + getClass().getName().replace('.', '/') + ".xml";
        context = new ClassPathXmlApplicationContext(resName);
        storage = createMock(SAMLMessageStorage.class);
        manager =  (MetadataManager) context.getBean("metadata", MetadataManager.class);
        resolver = (CredentialResolver) context.getBean("keyResolver", CredentialResolver.class);
        profile = new WebSSOProfileConsumerImpl(manager, resolver, "apollo");
        messageContext = new BasicSAMLMessageContext();
        builderFactory = Configuration.getBuilderFactory();
        helper = new WebSSOProfileTestHelper(builderFactory);
        messageContext.setInboundMessage(helper.getValidResponse());
    }

    /**
     * Verifies that in case SAML reponse object is missing from the cotext the processing fails.
     * @throws Exception error
     */
    @Test(expected = SAMLException.class)
    public void testMissingResponse() throws Exception {
        messageContext.setInboundMessage(null);
        profile.processResponse(messageContext, storage);
    }

    /**
     * Verifies that in case the response object is not of expceted type the processing will fail.
     * @throws Exception error
     */
    @Test(expected = SAMLException.class)
    public void testInvalidResponseObject() throws Exception {
        SAMLObjectBuilder<AuthnRequest> builder = (SAMLObjectBuilder<AuthnRequest>) builderFactory.getBuilder(AuthnRequest.DEFAULT_ELEMENT_NAME);
        AuthnRequest authnRequest = builder.buildObject();
        messageContext.setInboundMessage(authnRequest);
        profile.processResponse(messageContext, storage);
    }

    /**
     * Verifies that default authNStatement - currently created and expiring in three hours is accepted
     * by verification method.
     * @throws Exception error
     */
    @Test
    public void testDefaultAuthNStatementPasses() throws Exception {
        AuthnStatement statement = helper.getValidAuthStatement();
        profile.verifyAuthenticationStatement(statement, messageContext);
    }

    /**
     * Verifies that in case the session expiry time is in the past the statement is rejected.
     * @throws Exception error
     */
    @Test(expected = CredentialsExpiredException.class)
    public void testAuthNStatmenentWithExpiredSessionTime() throws Exception {
        AuthnStatement statement = helper.getValidAuthStatement();
        DateTime past = new DateTime();
        past.minusHours(3);
        statement.setSessionNotOnOrAfter(past);
        profile.verifyAuthenticationStatement(statement, messageContext);
    }

    private void verifyMock() {
        verify(storage);
    }

    private void replyMock() {
        replay(storage);
    }

}

