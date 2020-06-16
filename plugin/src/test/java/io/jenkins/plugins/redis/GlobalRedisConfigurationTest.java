/*
 * The MIT License
 *
 * Copyright (c) 2020, Sumit Sarin and Jenkins project contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.redis;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.GenericContainer;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GlobalRedisConfigurationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public GenericContainer redis = new GenericContainer<>("redis:6.0.4-alpine").withExposedPorts(6379);

    @Test
    public void configRoundTrip() throws Exception {
        JenkinsRule.WebClient web = j.createWebClient();
        HtmlForm form = web.goTo("configure").getFormByName("config");
        j.submit(form);

        GlobalRedisConfiguration g = GlobalRedisConfiguration.get();
        g.setSocketTimeout(3000);
        g.setConnectionTimeout(3000);
        g.setCredentialsId("dummy");
        g.setDatabase(0);
        g.setHost("dummy");
        g.setPort(3333);
        g.setSsl(true);
        g.save();
        g.load();

        form = web.goTo("configure").getFormByName("config");
        j.submit(form);
    }

    @Test
    public void testTestRedisConnection() throws IOException, ServletException {
        GlobalRedisConfiguration globalRedisConfiguration = GlobalRedisConfiguration.get();
        FormValidation result = globalRedisConfiguration.doTestRedisConnection("", 0, 0, false, "", 2000, 2000);
        assertThat(result.kind, is(FormValidation.Kind.ERROR));

        result = globalRedisConfiguration.doTestRedisConnection(redis.getHost(), redis.getFirstMappedPort(), 0, false,
                "", 2000, 2000);
        assertThat(result.kind, is(FormValidation.Kind.OK));
    }

}
