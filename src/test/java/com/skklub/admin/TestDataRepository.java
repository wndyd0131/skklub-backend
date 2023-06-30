package com.skklub.admin;

import com.skklub.admin.controller.dto.ClubResponseDTO;
import com.skklub.admin.controller.dto.S3DownloadDto;
import com.skklub.admin.domain.*;
import com.skklub.admin.domain.enums.Campus;
import com.skklub.admin.domain.enums.ClubType;
import com.skklub.admin.domain.enums.Role;
import com.skklub.admin.service.dto.ClubDetailInfoDto;
import com.skklub.admin.service.dto.ClubPrevDTO;
import com.skklub.admin.service.dto.FileNames;
import lombok.Getter;
import org.springframework.boot.test.context.TestComponent;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@TestComponent
public class TestDataRepository {

    private final int recruitCnt = 6;
    private final int clubCnt = 10;
    private final int activityImgPerClub = 5;
    private final List<Logo> logos = new ArrayList<>();
    private final List<ActivityImage> activityImages = new ArrayList<>();
    private final List<Club> clubs = new ArrayList<>();
    private final List<Recruit> recruits = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    public TestDataRepository() {
        readyUser();
        readyRecruit();
        readyLogo();
        readyActivityImages();
        readyClub();
    }

    public List<ActivityImage> getActivityImages(int clubIndex){
        return activityImages.subList(0 + activityImgPerClub * clubIndex, activityImgPerClub + activityImgPerClub * clubIndex);
    }

    public Club getCleanClub(int index) throws NoSuchFieldException, IllegalAccessException {
        Club club = clubs.get(index);
        club.setUser(null);
        club.getActivityImages().clear();
        Field logoField = club.getClass().getDeclaredField("logo");
        logoField.setAccessible(true);
        logoField.set(club, null);
        Field recruitField = club.getClass().getDeclaredField("recruit");
        recruitField.setAccessible(true);
        recruitField.set(club, null);
        return club;
    }

    public List<ClubDetailInfoDto> getClubDetailInfoDtos(){
        List<ClubDetailInfoDto> r = new ArrayList<>();
        for(long i = 0; i < clubCnt; i++){
            ClubDetailInfoDto dto = new ClubDetailInfoDto(clubs.get((int)i));
            dto.setId(i);
            r.add(dto);
        }
        return r;
    }

    public List<S3DownloadDto> getActivityImgS3DownloadDtos(int clubIndex) {
        List<ActivityImage> images = clubs.get(clubIndex).getActivityImages();
        List<S3DownloadDto> s3DownloadDtos = new ArrayList<>();
        for (int i = 0; i < activityImgPerClub; i++) {
            s3DownloadDtos.add(new S3DownloadDto((long) i, images.get(i).getOriginalName(), ("activityImgBytes" + i).getBytes()));
        }
        return s3DownloadDtos;
    }

    public S3DownloadDto getLogoS3DownloadDto(int clubIndex) {
        return new S3DownloadDto(
                (long) clubIndex,
                clubs.get(clubIndex).getLogo().getOriginalName(),
                "logoBytes".getBytes()
        );
    }

    public List<FileNames> getActivityImgFileNames(int clubIndex) {
        List<FileNames> fileNames = clubs.get(clubIndex).getActivityImages().stream()
                .map(FileNames::new)
                .collect(Collectors.toList());
        return fileNames;
    }

    public FileNames getLogoFileName(int clubIndex) {
        FileNames fileNames = new FileNames(clubs.get(clubIndex).getLogo());
        fileNames.setId(0L);
        return fileNames;
    }

    private void readyClub() {
        for (int i = 0; i < clubCnt; i++) {
            Club club = new Club(
                    "정상적인 클럽 SKKULOL" + i
                    , "1. 열심히 참여하면 됩니다 2. 그냥 게임만 잘 하면 됩니다." + i
                    , "취미교양"
                    , ClubType.중앙동아리
                    , "E-SPORTS" + i
                    , Campus.명륜
                    , "여기가 어떤 동아리냐면요, 페이커가 될 수 있게 해주는 동아리입니다^^" + i
                    , 2023 + i
                    , "명륜 게임 동아리입니다" + i
                    , "4학기" + i
                    , 60 + i
                    , "Thursday 19:00" + i
                    , "학생회관 80210" + i
                    , "www.skklol.com" + i
                    , "www.skkulol.edu" + i);
            Optional.ofNullable(logos.get(i)).ifPresent(club::changeLogo);
            List<ActivityImage> activityImagesTemp = new ArrayList<>();
            for (int j = 0; j < activityImgPerClub; j++) {
                activityImagesTemp.add(activityImages.get(j + i * activityImgPerClub));
            }
            club.appendActivityImages(activityImagesTemp);
            club.setUser(users.get(i));
            Optional.ofNullable(recruits.get(i)).ifPresent(club::startRecruit);
            clubs.add(club);
        }
    }

    private void readyLogo() {
        for (int i = 0; i < clubCnt; i++) {
            logos.add(new Logo("logoOriginal" + i + ".png", "logoSaved" + i + ".png"));
        }
    }

    private void readyActivityImages() {
        for (int i = 0; i < clubCnt * activityImgPerClub; i++) {
            activityImages.add(new ActivityImage("activityOriginal" + i + ".png", "activitySaved" + i + ".png"));
        }
    }

    private void readyRecruit() {
        for (int i = 0; i < recruitCnt; i++) {
            recruits.add(new Recruit(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), i + "명", "Test Recruit Process" + i, "010-" + String.valueOf(i).repeat(4) + "-" + String.valueOf(i).repeat(4), "Test Recruit web" + i));
        }
            for (int i = recruitCnt; i < clubCnt; i++) {
                recruits.add(null);
            }
    }

    private void readyUser() {
        for(int i = 0; i < clubCnt; i++)
            users.add(new User("userId" + i, "userPw" + i, Role.ROLE_USER, "userName" + i, "010-" + String.valueOf(i).repeat(4) + "-" + String.valueOf(i).repeat(4)));
    }
}
