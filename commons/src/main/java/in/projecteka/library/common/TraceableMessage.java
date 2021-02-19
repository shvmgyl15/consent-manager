package in.projecteka.library.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@AllArgsConstructor
@Builder
@Data
public class TraceableMessage implements Serializable {
    String correlationId;
    Object message;
}