package com.bilanee.octopus.adapter.facade;

import com.stellariver.milky.common.base.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("manage")
public class PaperFacade {

    /**
     * 试卷上传
     */
    @PostMapping("uploadPaper")
    public Result<Void> upload(MultipartFile file) throws IOException {
//        EasyExcel.read(file.getInputStream(), UploadData.class, new UploadDataListener(uploadDAO)).sheet().doRead();
        return Result.success();
    }
}
