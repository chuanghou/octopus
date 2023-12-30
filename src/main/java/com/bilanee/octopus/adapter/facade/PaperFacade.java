package com.bilanee.octopus.adapter.facade;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.bilanee.octopus.adapter.facade.vo.RawQuestion;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.basic.enums.QuestionType;
import com.bilanee.octopus.infrastructure.entity.PaperDO;
import com.bilanee.octopus.infrastructure.entity.Question;
import com.bilanee.octopus.infrastructure.mapper.PaperDOMapper;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping("manage")
public class PaperFacade {


    final PaperDOMapper paperDOMapper;

    /**
     * 试卷上传
     */
    @PostMapping("uploadPaper")
    public Result<Void> upload(MultipartFile file) throws IOException {

        paperDOMapper.selectList(null).forEach(paperDOMapper::deleteById);

        ExcelReader excelReader = EasyExcel.read(file.getInputStream()).build();
        List<ReadSheet> readSheets = excelReader.excelExecutor().sheetList();
        for (ReadSheet readSheet : readSheets) {
            String sheetName = readSheet.getSheetName();
            QuestionListener questionListener = new QuestionListener();
            EasyExcel.read(file.getInputStream(), RawQuestion.class, questionListener).sheet().doRead();
            if (Collect.size(questionListener.getRawQuestions()) != 50) {
                throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("试卷试题限制必须为50道"));
            }
            List<RawQuestion> rawQuestions = questionListener.getRawQuestions().subList(0, 50);
            List<Question> questions = rawQuestions.stream().map(r -> {
                List<String> options = Stream.of(r.getOptionA(), r.getOptionB(), r.getOptionC(), r.getOptionD()).filter(Kit::notBlank).collect(Collectors.toList());
                List<Choice> choices = new ArrayList<>();
                for (char c : r.getAnswers().toCharArray()) {
                    String s = String.valueOf(c);
                    choices.add(Kit.enumOfMightEx(Choice::name, s));
                }
                choices = choices.stream().sorted(Comparator.comparing(Choice::ordinal)).collect(Collectors.toList());
                return Question.builder()
                        .id(r.getId())
                        .type(Kit.enumOfMightEx(QuestionType::getDbCode, r.getType()))
                        .text(r.getText())
                        .options(options)
                        .answers(choices)
                        .build();
            }).collect(Collectors.toList());
            PaperDO paperDO = PaperDO.builder().name(sheetName).questions(questions).build();
            paperDOMapper.insert(paperDO);
        }
        return Result.success();
    }

    static class QuestionListener implements ReadListener<RawQuestion> {

        @Getter
        List<RawQuestion> rawQuestions = new ArrayList<>();

        @Override
        public void invoke(RawQuestion rawQuestion, AnalysisContext context) {
            rawQuestions.add(rawQuestion);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {

        }
    }
}
