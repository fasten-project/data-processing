/*
 * Copyright 2022 Software Improvement Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.sourcesprovider.data;

public class SourcePayload {
    private String forge;
    private String product;
    private String version;
    private String sourcePath;

    public SourcePayload(String forge, String product, String version, String sourcePath) {
        setForge(forge);
        setProduct(product);
        setVersion(version);
        setSourcePath(sourcePath);
    }

    public String getForge() {
        return forge;
    }

    public void setForge(String forge) {
        this.forge = forge;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
}
