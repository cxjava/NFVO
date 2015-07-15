/*
 * Copyright (c) 2015 Fraunhofer FOKUS
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

package org.project.openbaton.nfvo.core.interfaces;

import org.project.openbaton.catalogue.mano.descriptor.VNFForwardingGraphDescriptor;

import java.util.List;

/**
 * Created by lto on 16/06/15.
 */
public interface VNFFGManagement {
    VNFForwardingGraphDescriptor add(VNFForwardingGraphDescriptor vnfForwardingGraphDescriptor);

    void delete(String id);

    List<VNFForwardingGraphDescriptor> query();

    VNFForwardingGraphDescriptor query(String id);

    VNFForwardingGraphDescriptor update(VNFForwardingGraphDescriptor vnfForwardingGraphDescriptor, String id);
}
