package com.flysoft.fretcorridor.exe.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

/** RG-072/EF-EXE-05 : empreinte cryptographique conservée à côté de la clé objet. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoPreuve {

    private String objectKey;
    private String empreinteSha256;
}
