import React from "react";

function WelcomeCard() {
  return (
    <div
      style={{
        position: "relative",
        borderRadius: "12px",
        overflow: "hidden",
        marginBottom: "1rem",
      }}
    >
      <img
        src="https://picsum.photos/600/200"
        alt="welcome"
        style={{ width: "100%", height: "200px", objectFit: "cover" }}
      />
      <div
        style={{
          position: "absolute",
          top: "20px",
          left: "20px",
          color: "#fff",
        }}
      >
        <h3>홍길동님 어서오세요!</h3>
        <p style={{ color: "red" }}>구강 촬영일로부터 9일 3시간 지났어요.</p>
        <strong>구강 검진을 시작해볼까요?</strong>
      </div>
    </div>
  );
}

export default WelcomeCard;
