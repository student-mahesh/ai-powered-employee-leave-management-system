package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.AppUser;
import java.io.IOException;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PolicyIngestionService {
    private static final int CHUNK_SIZE = 1400;
    private final PolicyDocumentRepository documents;
    private final PolicyChunkRepository chunks;
    private final ObjectProvider<EmbeddingModel> embeddings;
    private final ChromaClient chroma;

    @Transactional
    public PolicyDocument ingest(MultipartFile file, String version, AppUser uploader) {
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("Policy PDF must be between 1 byte and 10 MB");
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) throw new IllegalArgumentException("Only PDF policy files are supported");
        try (var pdf = Loader.loadPDF(file.getBytes())) {
            var policy = documents.save(new PolicyDocument(file.getOriginalFilename(), version, uploader));
            var stripper = new PDFTextStripper();
            int index = 0;
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                var text = stripper.getText(pdf).trim();
                for (String part : split(text)) {
                    var vectorId = "policy-" + policy.getId() + "-" + index;
                    var model = embeddings.getIfAvailable();
                    if (model == null) throw new IllegalStateException("Ollama embedding model is unavailable; start Ollama before uploading policies");
                    var chromaId = chroma.upsert(vectorId, part, model.embed(part), java.util.Map.of("fileName", policy.getFileName(), "page", String.valueOf(page)));
                    chunks.save(new PolicyChunk(policy, index++, page, part, chromaId));
                }
            }
            return policy;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Policy PDF could not be read", ex);
        }
    }

    /** Idempotently repairs the vector linkage for one existing document. */
    @Transactional
    public int reindex(PolicyDocument policy) {
        var model = embeddings.getIfAvailable();
        if (model == null) throw new IllegalStateException("Ollama embedding model is unavailable; start Ollama before reindexing policies");
        int indexed = 0;
        for (var chunk : chunks.findByDocumentOrderByChunkIndex(policy)) {
            var vectorId = "policy-" + policy.getId() + "-" + chunk.getChunkIndex();
            var chromaId = chroma.upsert(vectorId, chunk.getContent(), model.embed(chunk.getContent()),
                    java.util.Map.of("fileName", policy.getFileName(), "page", String.valueOf(chunk.getSourcePage())));
            chunk.assignVectorId(chromaId);
            chunks.save(chunk);
            indexed++;
        }
        return indexed;
    }

    private ArrayList<String> split(String text) {
        var parts = new ArrayList<String>();
        for (int start = 0; start < text.length(); start += CHUNK_SIZE) parts.add(text.substring(start, Math.min(text.length(), start + CHUNK_SIZE)));
        return parts;
    }
}
