package com.example.lvcheng.service;
import com.example.lvcheng.dao.DiscussPostMapper;
import com.example.lvcheng.entity.DiscussPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 帖子相关
 */
@Service
public class DiscussPostService {

//    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired(required = false)
    private DiscussPostMapper discussPostMapper;

    /**
     * 分页查询讨论帖信息
     *
     * @param userId 当传入的 userId = 0 时查找所有用户的帖子
     *               当传入的 userId != 0 时，查找该指定用户的帖子
     * @param offset 每页的起始索引
     * @param limit  每页显示多少条数据
     * @return
     */
    public List<DiscussPost> findDiscussPosts (int userId, int offset, int limit) {
        // 查询数据库
//        logger.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    /**
     * 查询讨论贴的个数
     * @param userId 当传入的 userId = 0 时计算所有用户的帖子总数
     *               当传入的 userId ！= 0 时计算该指定用户的帖子总数
     * @return
     */
    public int findDiscussPostRows (int userId) {
        // 查询数据库
//        logger.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /**
     * 添加帖子
     * @param discussPost
     * @return
     */
    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 转义 HTML 标记，防止在 HTML 标签中注入攻击语句
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        // 过滤敏感词
//        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
//        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    /**
     * 根据 id 查询帖子
     * @param id
     * @return
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 修改帖子的评论数量
     * @param id 帖子 id
     * @param commentCount
     * @return
     */
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    /**
     * 修改帖子类型：0-普通; 1-置顶;
     * @param id
     * @param type
     * @return
     */
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    /**
     * 修改帖子状态：0-正常; 1-精华; 2-拉黑;
     * @param id
     * @param status
     * @return
     */
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    /**
     * 修改帖子分数
     * @param id
     * @param score
     * @return
     */
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

}
