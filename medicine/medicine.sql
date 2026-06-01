CREATE DATABASE medicine;
USE medicine;

-- 患者表（含登录凭证 + 健康档案）
create table patient (
    patientId integer not null auto_increment,
    patientName varchar(50) not null comment '患者姓名',
    password varchar(255) not null comment '密码',
    phone varchar(11) not null comment '手机号码',
    email varchar(255) not null comment '电子邮箱',
    age integer comment '年龄',
    allergiesHistory text comment '过敏史',
    habits text comment '生活习惯',
    oldHistory text comment '既往史',
    sex tinyint comment '性别（0：男，1：女）',
    createTime datetime default CURRENT_TIMESTAMP,
    primary key (patientId)
);

-- 医生表（含登录凭证 + 执业信息）
create table doctor (
    doctorId integer not null auto_increment,
    doctorName varchar(50) not null comment '医生姓名',
    password varchar(255) not null comment '密码',
    phone varchar(11) not null comment '手机号码',
    email varchar(255) not null comment '电子邮箱',
    cityName varchar(50) comment '所在地区',
    departmentName varchar(50) comment '所在科室',
    hospitalName varchar(50) comment '所在医院',
    introduction text,
    createTime datetime default CURRENT_TIMESTAMP,
    primary key (doctorId)
);

-- 病历表
create table record (
    recordId integer not null auto_increment,
    doctorId integer,
    patientId integer,
    description text not null comment '病情描述',
    tongue varchar(255) comment '舌像',
    status tinyint default 0 comment '病历状态（0：未查看，1：已查看，2：已诊断，3：诊断结束）',
    patientName varchar(255) comment '患者姓名',
    sex tinyint not null comment '性别（0：男，1：女）',
    age integer NOT NULL comment '年龄',
    allergiesHistory text comment '过敏史',
    habits text comment '生活习惯',
    oldHistory text comment '既往史',
    phone varchar(11) not null  comment '手机号码',
    score float default 0 comment '诊断评分',
    createTime datetime default CURRENT_TIMESTAMP,
    updateTime datetime default CURRENT_TIMESTAMP,
    primary key (recordId),
    FOREIGN KEY (doctorId) REFERENCES doctor(doctorId),
    FOREIGN KEY (patientId) REFERENCES patient(patientId)
);

-- 评论表（userId+role 标识评论者：role=0时userId=patientId，role=1时userId=doctorId）
create table comment (
    commentId integer not null auto_increment,
    content text comment '评论内容',
    createTime datetime default CURRENT_TIMESTAMP,
    role tinyint not null comment '用户角色（0：患者，1：医生）',
    username varchar(255) comment '发布评论用户姓名',
    recordId integer,
    userId integer,
    primary key (commentId),
    FOREIGN KEY (recordId) REFERENCES record(recordId)
);

-- 回复表
create table reply (
    replyId integer not null auto_increment,
    content text comment '回复内容',
    createTime datetime default CURRENT_TIMESTAMP,
    role tinyint not null comment '用户角色（0：患者，1：医生）',
    replyedName varchar(255),
    username varchar(255),
    commentId integer COMMENT '关联评论表（回复哪条评论）',
    userId integer,
    primary key (replyId),
    FOREIGN KEY (commentId) REFERENCES comment(commentId)
);

-- 诊断表
create table diagnostic (
    diagnosticId integer not null auto_increment,
    orders text comment '医嘱',
    prescription text comment '药方',
    result text comment '诊断结果',
    recordId integer,
    primary key (diagnosticId),
    FOREIGN KEY (recordId) REFERENCES record(recordId)
);
