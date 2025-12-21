package com.rag.project.api.component;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileHandler {
    /**
     * 파일에서 텍스트를 추출
     */
    public String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename().toLowerCase();

        //pdf 파일
        if(contentType.equals("application/pdf") || fileName.endsWith(".pdf")){
            try(PDDocument document = PDDocument.load(file.getInputStream())){
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }

        //텍스트 파일
        else if(contentType.startsWith("text/") || fileName.endsWith(".txt")){
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        //Word 파일
        else if(fileName.endsWith(".docx")){
            return extractFromWord(file);
        }

        //PPT 파일
        else if(fileName.endsWith(".pptx")){
            return extractFromPpt(file);
        }
        else{
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + contentType);
        }
    }
    //Word 파일 텍스트 추출
    private String extractFromWord(MultipartFile file) throws IOException{
        try(XWPFDocument doc = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc)){
            return extractor.getText();
        }
    }
    // PPT 파일 텍스트 추출
    private String extractFromPpt(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();

        // XMLSlideShow: .pptx 파일을 읽는 POI 클래스
        try (XMLSlideShow ppt = new XMLSlideShow(file.getInputStream())) {
            // 슬라이드 하나씩 순회
            for (XSLFSlide slide : ppt.getSlides()) {
                // 슬라이드 안에 있는 도형(Shape)들 중에서 '글 상자'만 찾음
                slide.getShapes().stream()
                        .filter(shape -> shape instanceof XSLFTextShape) // 텍스트가 있는 도형인가?
                        .map(shape -> (XSLFTextShape) shape)             // 형변환
                        .forEach(textShape -> sb.append(textShape.getText()).append("\n")); // 텍스트 꺼내서 합치기
            }
        }
        return sb.toString();
    }
    //텍스트 분할 메서드
    public List<String> splitTextIntoChunks(String text, int chunkSize){
        List<String> chunks = new ArrayList<>();
        for(int i = 0; i < text.length(); i += chunkSize){
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    //텍스트 정제 함수
    public String sanitizeText(String text){
        if (text == null){
            return null;
        }
        //0x00를 빈 문자열로 제거
        return text.replace("\u0000", "");
    }
}
