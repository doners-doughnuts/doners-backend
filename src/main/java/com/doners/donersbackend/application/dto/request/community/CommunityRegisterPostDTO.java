package com.doners.donersbackend.application.dto.request.community;

import com.sun.istack.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@ApiModel("CommunityRegisterPostDTO")
@NoArgsConstructor
@Getter
public class CommunityRegisterPostDTO {

    @NotBlank
    @NotNull
    @ApiModelProperty(name="글 제목")
    private String communityTitle;

    @NotBlank
    @NotNull
    @ApiModelProperty(name="글 내용")
    private String communityDescription;
}
