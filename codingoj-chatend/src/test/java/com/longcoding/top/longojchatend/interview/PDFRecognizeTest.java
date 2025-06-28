package com.longcoding.top.longojchatend.interview;

import com.longcoding.top.service.IPDFRecognizeService;
import jakarta.annotation.Resource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PDFRecognizeTest {

    @Resource
    private IPDFRecognizeService pdfRecognizeService;

    // @Test
    public void testPDFRecognize() {
        // 测试识别PDF文件中的文本内容
        // 这里可以调用IPDFRecognizeService的实现类方法进行测试
        String result = pdfRecognizeService.recognizeTextFromPDF("http://longoj-1350136079.cos.ap-nanjing.myqcloud.com/user_resume/1/vaamFDff-Java-LongWei.pdf");
        System.out.println(result);
    }

}
