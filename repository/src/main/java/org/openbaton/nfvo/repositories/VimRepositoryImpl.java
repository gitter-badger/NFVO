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

package org.openbaton.nfvo.repositories;

import org.openbaton.catalogue.nfvo.NFVImage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

/**
 * Created by dbo on 21/09/15.
 */
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class VimRepositoryImpl implements VimRepositoryCustom {

    @Autowired
    private VimRepository vimRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Override
    @Transactional
    public NFVImage addImage(String id, NFVImage image) {
        image = imageRepository.save(image);
        vimRepository.findFirstById(id).getImages().add(image);
        return image;
    }

    @Override
    @Transactional
    public void deleteImage(String idVim, String idImage) {
        vimRepository.findFirstById(idVim).getImages().remove(imageRepository.findOne(idImage));
        imageRepository.delete(idImage);
    }
}
