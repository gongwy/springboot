package com.xwbing.domain.entity.model;

import lombok.Data;

import java.util.List;

/**
 * 说明: 实体模型
 * 项目名称: boot-module-demo
 * 创建时间: 2017/5/10 16:36
 * 作者:  xiangwb
 */
@Data
public class EntityModel {
    private String character ;
    private int number;
    private Integer inte;
    private double decimal;
    private boolean b;
    private List list;
}
