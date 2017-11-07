package com.xwbing.service;

import com.alibaba.fastjson.JSONObject;
import com.xwbing.constant.CommonConstant;
import com.xwbing.constant.CommonEnum;
import com.xwbing.entity.SysConfig;
import com.xwbing.entity.SysUser;
import com.xwbing.util.CommonDataUtil;
import com.xwbing.entity.model.EmailModel;
import com.xwbing.exception.BusinessException;
import com.xwbing.repository.UserRepository;
import com.xwbing.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 说明:
 * 项目名称: boot-module-demo
 * 创建时间: 2017/5/5 16:44
 * 作者:  xiangwb
 */
@Service
public class UserService {
    @Resource
    private UserRepository userRepository;
    @Resource
    private SysConfigService sysConfigService;

    /**
     * 增
     *
     * @param sysUser
     * @return
     */
    public RestMessage save(SysUser sysUser) {
        RestMessage result = new RestMessage();
        SysUser old = findByUserName(sysUser.getUserName());
        if (old != null) {
            throw new BusinessException("已经存在此用户名");
        }
        sysUser.setId(PassWordUtil.createId());
        sysUser.setCreateTime(new Date());
        // 获取初始密码
        String[] res = PassWordUtil.getUserSecret(null, null);
        sysUser.setSalt(res[1]);
        sysUser.setPassword(res[2]);
        // 设置否管理员
        sysUser.setAdmin(CommonEnum.YesOrNoEnum.NO.getCode());
        SysUser one = userRepository.save(sysUser);
        if (one == null) {
            throw new BusinessException("新增用户失败");
        }
        //发送邮件
        boolean send = sendEmail(sysUser, res[0]);
        // 发送邮件结束
        if (!send) {
            throw new BusinessException("发送密码邮件错误");
        }
        result.setSuccess(true);
        return result;
    }

    /**
     * 删
     *
     * @param id
     * @return
     */
    public RestMessage removeById(String id) {
        RestMessage result = new RestMessage();
        SysUser old = findOne(id);
        if (old == null) {
            throw new BusinessException("该对象不存在");
        }
        if (id.equals(CommonDataUtil.getToken(CommonConstant.CURRENT_USER_ID))) {
            throw new BusinessException("不能删除当前登录用户");
        }
        if (CommonEnum.YesOrNoEnum.YES.getCode().equals(old.getAdmin())) {
            throw new BusinessException("不能对管理员进行删除操作");
        }
        userRepository.delete(id);
        result.setMessage("删除成功");
        result.setSuccess(true);
        return result;

    }

    /**
     * 更新
     *
     * @param sysUser
     * @return
     */
    public RestMessage update(SysUser sysUser) {
        RestMessage result = new RestMessage();
        SysUser old = findOne(sysUser.getId());
        if (old == null) {
            throw new BusinessException("该对象不存在");
        }
        if (old.getId().equals(CommonDataUtil.getToken(CommonConstant.CURRENT_USER_ID))) {
            throw new BusinessException("不能修改当前登录用户");
        }
        if (CommonEnum.YesOrNoEnum.YES.getCode().equals(old.getAdmin())) {
            throw new BusinessException("不能对管理员进行修改操作");
        }
        // 根据实际情况补充
        old.setName(sysUser.getName());
        old.setMail(sysUser.getMail());
        old.setSex(sysUser.getSex());
        old.setModifiedTime(new Date());
//        old.setUserName(sysUser.getUserName());//用户名不能修改
        SysUser one = userRepository.save(old);
        if (one != null) {
            result.setMessage("更新成功");
            result.setSuccess(true);
        } else {
            result.setMessage("更新失败");
        }
        return result;
    }

    /**
     * 单个查询
     *
     * @param id
     * @return
     */
    public SysUser findOne(String id) {
        return userRepository.findOne(id);
    }

    /**
     * 列表查询
     *
     * @return
     */
    public List<SysUser> listAll() {
        List<SysUser> all = userRepository.findAll();
        if (CollectionUtils.isNotEmpty(all)) {
            all.forEach(sysUser -> {
                CommonEnum.SexEnum sexEnum = Arrays.stream(CommonEnum.SexEnum.values()).filter(obj -> obj.getCode().equals(sysUser.getSex())).findFirst().get();
                sysUser.setSexName(sexEnum.getName());
                sysUser.setCreate(DateUtil2.dateToStr(sysUser.getCreateTime(), DateUtil2.YYYY_MM_DD_HH_MM_SS));
                Date modifiedTime = sysUser.getModifiedTime();
                if (modifiedTime != null)
                    sysUser.setModified(DateUtil2.dateToStr(sysUser.getModifiedTime(), DateUtil2.YYYY_MM_DD_HH_MM_SS));
            });
        }
        return all;
    }

    /**
     * 重置密码
     *
     * @param id
     * @return
     */
    public RestMessage resetPassWord(String id) {
        RestMessage result = new RestMessage();
        SysUser old = findOne(id);
        if (old == null)
            throw new BusinessException("未查询到用户信息");
        if (CommonDataUtil.getToken(CommonConstant.CURRENT_USER_ID).equals(id))
            throw new BusinessException("不能重置当前登录用户");
        if (CommonEnum.YesOrNoEnum.YES.getCode().equals(old.getAdmin())) {
            throw new BusinessException("管理员密码不能重置");
        }
        String[] str = PassWordUtil.getUserSecret(null, null);
        old.setSalt(str[1]);
        old.setPassword(str[2]);
        old.setModifiedTime(new Date());
        SysUser save = userRepository.save(old);
        if (save == null)
            throw new BusinessException("重置密码失败");
        boolean send = sendEmail(old, str[0]);
        if (!send)
            throw new BusinessException("发送密码邮件错误");
        result.setSuccess(true);
        result.setMessage("重置密码成功");
        return result;
    }

    /**
     * 修改密码
     *
     * @param newPassWord
     * @param oldPassWord
     * @return
     */
    public RestMessage updatePassWord(String newPassWord, String oldPassWord, String id) {
        RestMessage result = new RestMessage();
        SysUser sysUser = findOne(id);
        if (sysUser == null)
            throw new BusinessException("该用户不存在");
        boolean flag = checkPassWord(oldPassWord, sysUser.getPassword(), sysUser.getSalt());
        if (!flag)
            throw new BusinessException("原密码错误,请重新输入");
        String[] str = PassWordUtil.getUserSecret(newPassWord, null);
        sysUser.setSalt(str[1]);
        sysUser.setPassword(str[2]);
        sysUser.setModifiedTime(new Date());
        SysUser save = userRepository.save(sysUser);
        if (save != null) {
            result.setMessage("修改密码成功");
            result.setSuccess(true);
        } else {
            result.setMessage("修改密码失败");
        }
        return result;
    }

    /**
     * 登录
     *
     * @param userName
     * @param passWord
     * @param checkCode
     * @return
     */
    public RestMessage login(String userName, String passWord, String checkCode) {
        RestMessage restMessage = new RestMessage();
        String imgCode = (String) CommonDataUtil.getToken(CommonConstant.KEY_CAPTCHA);
        //验证验证码
        if (!checkCode.equalsIgnoreCase(imgCode)) {
            throw new BusinessException("验证码错误");
        }
        //验证账号,密码
        SysUser user = findByUserName(userName);
        if (user == null)
            throw new BusinessException("账号错误");
        boolean flag = checkPassWord(passWord, user.getPassword(), user.getSalt());
        if (!flag)
            throw new BusinessException("密码错误");
        CommonDataUtil.setToken(CommonConstant.CURRENT_USER, userName);
        CommonDataUtil.setToken(CommonConstant.CURRENT_USER_ID, user.getId());
        restMessage.setSuccess(true);
        restMessage.setMessage("登录成功");
        return restMessage;
    }

    /**
     * 根据用户名查找用户
     *
     * @param userName
     * @return
     */
    private SysUser findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    /**
     * 校验密码
     *
     * @param realPassWord
     * @param passWord
     * @param salt
     * @return
     */
    private boolean checkPassWord(String passWord, String realPassWord, String salt) {
        // 根据密码盐值， 解码
        byte[] saltByte = EncodeUtils.hexDecode(salt);
        byte[] hashPassword = Digests.sha1(passWord.getBytes(), saltByte,
                PassWordUtil.HASH_INTERATIONS);
        // 密码 数据库中密码
        String validatePassWord = EncodeUtils.hexEncode(hashPassword);
        //判断密码是否相同
        return realPassWord.equals(validatePassWord);
    }

    /**
     * 发送邮件
     *
     * @param sysUser
     * @param passWord
     * @return
     */
    private boolean sendEmail(SysUser sysUser, String passWord) {
        SysConfig sysConfig = sysConfigService.findByCode(CommonConstant.EMAIL_KEY);
        if (sysConfig == null) {
            throw new BusinessException("没有查找到邮件配置项");
        }
        JSONObject jsonObject = JSONObject.parseObject(sysConfig.getValue());
        EmailModel emailModel = JSONObject.toJavaObject(jsonObject, EmailModel.class);
        emailModel.setToEmail(sysUser.getMail());
        emailModel.setSubject(emailModel.getSubject());
        emailModel.setCentent(emailModel.getCentent() + " 你的用户名是:" + sysUser.getUserName() + ",密码是:" + passWord);
        return EmailUtil.sendTextEmail(emailModel);
    }
}
