package com.bilanee.octopus.adapter.facade;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.bilanee.octopus.adapter.facade.vo.Question;
import com.stellariver.milky.common.base.Result;
import lombok.Getter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("manage")
public class PaperFacade {

    /**
     * 试卷上传
     */
    @PostMapping("uploadPaper")
    public Result<Void> upload(MultipartFile file) throws IOException {
        QuestionListener questionListener = new QuestionListener();
        EasyExcel.read(file.getInputStream(), Question.class, questionListener).sheet().doRead();
        List<Question> questions = questionListener.getQuestions();
        System.out.println(questions);
        return Result.success();
    }


    static class QuestionListener implements ReadListener<Question> {

        @Getter
        List<Question> questions = new ArrayList<>();

        @Override
        public void invoke(Question question, AnalysisContext context) {
            questions.add(question);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {

        }
    }
}
