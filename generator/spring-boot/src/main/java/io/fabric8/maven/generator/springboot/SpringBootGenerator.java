/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.maven.generator.springboot;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.maven.core.util.Configs;
import io.fabric8.maven.core.util.MavenUtil;
import io.fabric8.maven.docker.config.AssemblyConfiguration;
import io.fabric8.maven.docker.config.BuildImageConfiguration;
import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.generator.api.BaseGenerator;
import io.fabric8.maven.generator.api.FromSelector;
import io.fabric8.maven.generator.api.MavenGeneratorContext;
import io.fabric8.utils.Strings;
import org.apache.maven.project.MavenProject;

/**
 * @author roland
 * @since 15/05/16
 */
public class SpringBootGenerator extends BaseGenerator {

    private static final String IMAGE_JAVA_VERSION = "1.1.10";
    private static final String IMAGE_S2I_JAVA_VERSION = "1.3.3";

    public SpringBootGenerator(MavenGeneratorContext context) {
        super(context, "spring-boot", new FromSelector.Default(context,
                                                               "fabric8/java-alpine-openjdk8-jdk:" + IMAGE_JAVA_VERSION, "fabric8/s2i-java:" + IMAGE_S2I_JAVA_VERSION,
                                                               "jboss-fuse-6/fis-java-openshift", "jboss-fuse-6/fis-java-openshift"));
    }

    private enum Config implements Configs.Key {
        webPort        {{ d = "8080"; }},
        jolokiaPort    {{ d = "8778"; }},
        prometheusPort {{ d = "9779"; }};

        public String def() { return d; } protected String d;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs) {
        if (isApplicable() && shouldAddDefaultImage(configs)) {
            ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();
            BuildImageConfiguration.Builder buildBuilder = new BuildImageConfiguration.Builder()
                .assembly(createAssembly())
                .from(getFrom())
                .ports(extractPorts());
            addLatestTagIfSnapshot(buildBuilder);
            imageBuilder
                .name(getImageName())
                .alias(getAlias())
                .buildConfig(buildBuilder.build());
            configs.add(imageBuilder.build());
            return configs;
        } else {
            return configs;
        }
    }

    @Override
    public boolean isApplicable() {
        MavenProject project = getProject();
        return MavenUtil.hasPlugin(project,"org.springframework.boot:spring-boot-maven-plugin");
    }

    private List<String> extractPorts() {
        // TODO would rock to look at the base image and find the exposed ports!
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.webPort));
        addPortIfValid(answer, getConfig(Config.jolokiaPort));
        addPortIfValid(answer, getConfig(Config.prometheusPort));
        return answer;
    }

    private void addPortIfValid(List<String> list, String port) {
        if (Strings.isNotBlank(port) && Integer.parseInt(port) != 0) {
            list.add(port);
        }
    }

    private AssemblyConfiguration createAssembly() {
        return
            new AssemblyConfiguration.Builder()
                .basedir("/app")
                .descriptorRef("artifact-with-includes")
                .build();
    }
}
