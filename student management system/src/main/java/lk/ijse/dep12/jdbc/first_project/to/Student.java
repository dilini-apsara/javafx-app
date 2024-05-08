package lk.ijse.dep12.jdbc.first_project.to;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private String id;
    private String name;
    private String address;
    private String contact;
}
