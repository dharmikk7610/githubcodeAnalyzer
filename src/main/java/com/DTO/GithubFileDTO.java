package com.DTO;

import lombok.Data;

@Data
public class GithubFileDTO {

    private String name;

    private String path;

    private String type;

    private String download_url;

}